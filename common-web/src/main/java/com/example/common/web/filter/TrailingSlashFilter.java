package com.example.common.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to normalize URLs by stripping trailing slashes.
 * <p>
 * Spring Boot 3+ / Spring Framework 6+ removed automatic trailing slash matching
 * (previously enabled via {@code setUseTrailingSlashMatch(true)}). This filter
 * provides a global solution by stripping trailing slashes from incoming request URIs.
 * <p>
 * Example: {@code /api/profile/} is treated as {@code /api/profile}
 * <p>
 * This eliminates the need for duplicate path mappings like:
 * <pre>
 * {@literal @}GetMapping({"/profile", "/profile/"})
 * </pre>
 * <p>
 * <b>Note:</b> As of Spring Boot 3.x/4.x, there is no global configuration property
 * to restore the old trailing slash behavior. This filter is the recommended workaround.
 *
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/28552">Spring Framework Issue #28552</a>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrailingSlashFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Strip trailing slash (except for root "/")
        if (uri.length() > 1 && uri.endsWith("/")) {
            String normalizedUri = uri.substring(0, uri.length() - 1);
            HttpServletRequest wrappedRequest = new TrailingSlashRequestWrapper(request, normalizedUri);
            filterChain.doFilter(wrappedRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Request wrapper that overrides the URI to strip the trailing slash.
     */
    private static class TrailingSlashRequestWrapper extends HttpServletRequestWrapper {

        private final String normalizedUri;

        public TrailingSlashRequestWrapper(HttpServletRequest request, String normalizedUri) {
            super(request);
            this.normalizedUri = normalizedUri;
        }

        @Override
        public String getRequestURI() {
            return normalizedUri;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer();
            url.append(getScheme()).append("://").append(getServerName());
            if (getServerPort() != 80 && getServerPort() != 443) {
                url.append(":").append(getServerPort());
            }
            url.append(normalizedUri);
            return url;
        }
    }
}
