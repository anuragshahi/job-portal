package com.example.bff.controller;

import com.example.common.core.constant.SessionConstants;
import com.example.bff.service.SessionRedisService;
import com.example.bff.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Backend-for-Frontend (BFF) controller handling OAuth2 authentication and API proxying.
 * <p>
 * This controller manages two distinct authentication mechanisms:
 * <ul>
 *   <li><b>Spring Security OAuth2 session</b> - Standard JSESSIONID-based session created
 *       during OAuth2 login. Used by the /user endpoint to return identity claims.</li>
 *   <li><b>Custom BFF session</b> - BFF_SESSION cookie containing a signed JWT with a JTI
 *       that maps to OAuth2 tokens stored in Redis. Used by /api/** proxy endpoints.</li>
 * </ul>
 * <p>
 * The dual mechanism exists because:
 * <ul>
 *   <li>/user needs Spring Security's OAuth2 integration to access OIDC claims</li>
 *   <li>/api/** needs custom session handling for proactive token refresh and
 *       fine-grained control over the gateway proxy behavior</li>
 * </ul>
 * <p>
 * Both mechanisms must be properly cleared during logout to prevent authentication leaks.
 */
@RestController
@RequestMapping("/bff")
@RequiredArgsConstructor
public class BffController {

    private final OAuth2AuthorizedClientService clientService;
    private final SessionRedisService sessionService;
    private final JwtUtils jwtUtils;
    private final WebClient.Builder webClientBuilder;
    private final Environment env;

    @Value("${bff.gateway.url}")
    private String gatewayUrl;

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
    private String issuerUri;

    @Value("${bff.cookie.secure}")
    private boolean cookieSecure;

    @Value("${bff.frontend.url}")
    private String frontendUrl;

    /**
     * Returns the authenticated user's identity claims from Keycloak.
     * <p>
     * This endpoint provides OIDC identity information (sub, email, preferred_username, name)
     * from the Identity Provider. It is used by the frontend to check authentication state
     * and display basic user info in the UI.
     * <p>
     * Note: This is separate from the Profile Service which stores application-specific
     * business data. The IdP is the source of truth for identity; the Profile Service
     * is the source of truth for application domain data.
     *
     * @param user The authenticated OIDC user (injected by Spring Security)
     * @return The OIDC user claims, or triggers 401 if not authenticated
     */
    @GetMapping("/user")
    public OidcUser user(@AuthenticationPrincipal OidcUser user) {
        return user;
    }

    @GetMapping("/login")
    public void login(HttpServletResponse res) throws IOException {
        res.sendRedirect("/oauth2/authorization/keycloak");
    }

    /**
     * Handles successful OAuth2 login by creating a BFF session.
     * <p>
     * This endpoint is called after Keycloak redirects back with an authorization code
     * and Spring Security exchanges it for tokens. It:
     * <ol>
     *   <li>Stores the OAuth2 tokens in Redis (keyed by a unique JTI)</li>
     *   <li>Stores the ID token separately for use during logout</li>
     *   <li>Issues a signed BFF_SESSION JWT cookie containing the JTI</li>
     *   <li>Redirects to the frontend application</li>
     * </ol>
     *
     * @param auth The OAuth2 authentication token from Spring Security
     * @return Redirect to frontend with BFF_SESSION cookie set
     */
    @GetMapping("/login/success")
    public ResponseEntity<?> loginSuccess(OAuth2AuthenticationToken auth) {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                auth.getAuthorizedClientRegistrationId(), auth.getName());

        String jti = UUID.randomUUID().toString();
        sessionService.save(jti, client);

        // Store ID token for Keycloak logout
        if (auth.getPrincipal() instanceof OidcUser oidcUser) {
            String idTokenValue = oidcUser.getIdToken().getTokenValue();
            sessionService.saveIdToken(jti, idTokenValue);
        }

        String sessionJwt = jwtUtils.issueSessionJwt(jti, auth);

        ResponseCookie cookie = ResponseCookie.from(SessionConstants.COOKIE_BFF_SESSION, sessionJwt)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .build();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, frontendUrl)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    /**
     * Performs complete logout across all authentication layers.
     * <p>
     * The BFF maintains two authentication mechanisms that must both be cleared:
     * <ul>
     *   <li><b>Spring Security session (JSESSIONID)</b> - Used by /bff/user endpoint</li>
     *   <li><b>BFF_SESSION cookie + Redis</b> - Used by /bff/api/** proxy endpoints</li>
     * </ul>
     * <p>
     * This endpoint:
     * <ol>
     *   <li>Extracts JTI from BFF_SESSION and deletes the Redis session</li>
     *   <li>Clears Spring SecurityContext and invalidates HTTP session</li>
     *   <li>Clears both BFF_SESSION and JSESSIONID cookies</li>
     *   <li>Redirects to Keycloak logout to invalidate the IdP session</li>
     *   <li>Keycloak then redirects back to the frontend login page</li>
     * </ol>
     *
     * @param sessionJwt The BFF_SESSION JWT cookie (optional, may be expired/missing)
     * @param request The HTTP request for session access
     * @return Redirect to Keycloak logout endpoint
     */
    @GetMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = SessionConstants.COOKIE_BFF_SESSION, required = false) String sessionJwt,
            HttpServletRequest request) {

        String idToken = null;

        if (sessionJwt != null) {
            try {
                String jti = jwtUtils.extractJti(sessionJwt);
                if (jti != null) {
                    idToken = sessionService.loadIdToken(jti);
                    sessionService.delete(jti);
                }
            } catch (Exception e) {
                // Ignore parsing errors on logout
            }
        }

        // Invalidate Spring Security session
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        ResponseCookie cookie = ResponseCookie.from(SessionConstants.COOKIE_BFF_SESSION, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        // Redirect to Keycloak logout endpoint
        String logoutUrl;
        if (idToken != null) {
            String redirectUri = frontendUrl + "/login";
            logoutUrl = issuerUri + "/protocol/openid-connect/logout?id_token_hint=" + idToken
                    + "&post_logout_redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        } else {
            // Fallback if no ID token (session expired or already logged out)
            logoutUrl = issuerUri + "/protocol/openid-connect/logout";
        }

        // Also clear JSESSIONID cookie
        ResponseCookie jsessionCookie = ResponseCookie.from(SessionConstants.COOKIE_JSESSIONID, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, logoutUrl)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.SET_COOKIE, jsessionCookie.toString())
                .build();
    }

    @RequestMapping(value = "/api/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<?> proxyRequest(
            HttpServletRequest request,
            @CookieValue(name = SessionConstants.COOKIE_BFF_SESSION, required = false) String sessionJwt,
            @RequestBody(required = false) byte[] body) {

        if (sessionJwt == null) {
            return buildErrorResponse(401, "MISSING_SESSION", "BFF_SESSION cookie not found");
        }

        String jti = jwtUtils.extractJti(sessionJwt);
        if (jti == null) {
            return buildErrorResponse(401, "INVALID_SESSION", "Failed to extract JTI from session JWT");
        }

        OAuth2AuthorizedClient client = sessionService.load(jti);
        if (client == null) {
            return buildErrorResponse(401, "SESSION_NOT_FOUND", "Session not found in Redis (expired or invalid)");
        }

        String accessToken = client.getAccessToken().getTokenValue();

        // Extract the path after "/bff/api"
        // Example: /bff/api/profile -> /profile, /bff/api/profile/foo -> /profile/foo
        String requestUri = request.getRequestURI();
        String path = requestUri.substring(requestUri.indexOf("/api") + 4); // Strip "/api"

        String queryString = request.getQueryString();
        URI targetUri = URI.create(gatewayUrl + path + (queryString != null ? "?" + queryString : ""));

        WebClient wc = webClientBuilder.build();

        try {
            return wc.method(HttpMethod.valueOf(request.getMethod()))
                    .uri(targetUri)
                    .headers(h -> {
                        h.setBearerAuth(accessToken);
                        // Forward Content-Type header (critical for POST/PUT requests with bodies)
                        // so the downstream service knows how to parse the payload (e.g., application/json).
                        if (request.getContentType() != null) {
                            h.setContentType(MediaType.parseMediaType(request.getContentType()));
                        }
                    })
                    .bodyValue(body != null ? body : new byte[0])
                    .retrieve()
                    .toEntity(byte[].class)
                    .block();
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getResponseBodyAsByteArray());
        }
    }

    /**
     * Proxies public requests (registration, confirmation, api-docs) without authentication.
     * <p>
     * URL pattern: /bff/public/{service}/{path}
     * Example: /bff/public/profile/register → Gateway /profile/public/register
     * <p>
     * This follows the unified public endpoint pattern where all public paths are
     * accessed via /{service}/public/** at the gateway level.
     */
    @RequestMapping(value = "/public/{service}/**", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> proxyPublicRequest(
            HttpServletRequest request,
            @PathVariable String service,
            @RequestBody(required = false) byte[] body) {

        // Extract the path after "/bff/public/{service}"
        String requestUri = request.getRequestURI();
        String prefix = "/bff/public/" + service;
        String remainingPath = requestUri.substring(requestUri.indexOf(prefix) + prefix.length());

        // Transform: /bff/public/profile/register → /profile/public/register
        String targetPath = "/" + service + "/public" + remainingPath;

        String queryString = request.getQueryString();
        URI targetUri = URI.create(gatewayUrl + targetPath + (queryString != null ? "?" + queryString : ""));

        WebClient wc = webClientBuilder.build();

        try {
            return wc.method(HttpMethod.valueOf(request.getMethod()))
                    .uri(targetUri)
                    .headers(h -> {
                        if (request.getContentType() != null) {
                            h.setContentType(MediaType.parseMediaType(request.getContentType()));
                        }
                    })
                    .bodyValue(body != null ? body : new byte[0])
                    .retrieve()
                    .toEntity(byte[].class)
                    .block();
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getResponseBodyAsByteArray());
        }
    }

    /**
     * Builds an error response with descriptive details in non-production environments.
     * In production, returns a generic 401 response to avoid leaking internal details.
     */
    private ResponseEntity<?> buildErrorResponse(int status, String errorCode, String message) {
        if (env.acceptsProfiles(Profiles.of("prod"))) {
            return ResponseEntity.status(status).build();
        }
        return ResponseEntity.status(status)
                .body(Map.of(
                    "status", status,
                    "error", errorCode,
                    "message", message
                ));
    }
}