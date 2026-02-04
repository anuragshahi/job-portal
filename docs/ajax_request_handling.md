# AJAX Request Handling (401 vs Redirect)

This document describes the architectural solution for handling authentication challenges in a way that is compatible with both standard browser navigation and modern Single Page Applications (SPAs).

## The Problem: Legacy Redirects in Modern Apps

By default, Spring Security (and most OAuth2 clients) handle unauthenticated requests by returning an **HTTP 302 Redirect** to the login page. While this works perfectly for traditional server-side rendered (SSR) websites, it creates major issues for SPAs like Angular:

1.  **Invisible Redirects:** The browser's `fetch` or `XMLHttpRequest` engine follows the 302 redirect automatically in the background.
2.  **HTML vs JSON Mismatch:** The Angular app expects a JSON response from an API call, but instead receives the **HTML code** of the Keycloak login page.
3.  **Application Crashes:** The Angular application typically fails with a "Unexpected token '<' in JSON" error because it cannot parse the login page as data.
4.  **CORS Issues:** Redirecting an AJAX call to a different domain (Keycloak) often triggers CORS violations that are difficult to debug.

## The Solution: Intelligent 401 Responses

The BFF is configured to detect the *nature* of the incoming request and respond with the appropriate status code:

*   **Browser Navigation:** If a user types a URL directly, return an **HTTP 302 Redirect**.
*   **AJAX/API Call:** If a script makes a background call, return an **HTTP 401 Unauthorized**.

---

## Server-Side Implementation (BFF)

In `SecurityConfig.java`, we implement a `DelegatingAuthenticationEntryPoint`. This component inspects the request headers before deciding on the response.

### Priority Rules:
1.  **X-Requested-With:** If the header is set to `XMLHttpRequest`, return **401**.
2.  **Accept Header:** If the request explicitly asks for `application/json`, return **401**.
3.  **Specific Endpoints:** Requests to `/bff/user` (the auth check endpoint) always return **401** on failure.
4.  **Default:** For all other cases, return **302 Redirect** to Keycloak.

```java
private AuthenticationEntryPoint authenticationEntryPoint() {
    LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints = new LinkedHashMap<>();

    // Detect AJAX via the 'X-Requested-With' header
    entryPoints.put(
        new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest"),
        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
    );

    // Detect API calls via 'Accept: application/json'
    entryPoints.put(
        new RequestHeaderRequestMatcher("Accept", "application/json"),
        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
    );

    // Default: Redirect to Login
    DelegatingAuthenticationEntryPoint entryPoint = new DelegatingAuthenticationEntryPoint(entryPoints);
    entryPoint.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/keycloak"));
    return entryPoint;
}
```

---

## Client-Side Implementation (Angular)

To ensure the BFF always identifies background requests correctly, the Angular application uses an `HttpInterceptor`.

### 1. Adding the Header
The interceptor clones every outgoing request and adds the `X-Requested-With` header:

```typescript
const authReq = req.clone({
  setHeaders: {
    'X-Requested-With': 'XMLHttpRequest'
  }
});
```

### 2. Handling the 401
The interceptor also listens for 401 responses. When one is detected, it triggers a clean redirect to the login flow:

```typescript
return next(authReq).pipe(
  catchError((error: HttpErrorResponse) => {
    if (error.status === 401) {
      // Cleanly navigate to login
      window.location.href = '/bff/login';
    }
    return throwError(() => error);
  })
);
```

## Benefits
*   **Reliability:** The Angular app never attempts to parse an HTML login page as JSON.
*   **User Experience:** The user is only redirected when the application is ready to handle the transition.
*   **Debugging:** 401 errors appear clearly in the Network tab, making it obvious when a session has expired.
