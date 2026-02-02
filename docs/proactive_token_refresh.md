# Proactive Token Refresh (Defense in Depth)

This document describes the strategy for handling "Mid-Flight Token Expiration" in our distributed system.

## The Problem: Mid-Flight Expiration

In a Defense in Depth architecture using JWKS, every microservice validates the Access Token.

1.  **Gateway** receives request. Token expires in **2 seconds**. Gateway validates: **OK**.
2.  Gateway forwards to **Order Service**. Network latency + processing takes **3 seconds**.
3.  **Order Service** receives request. Token is now **expired**. Order Service validates: **FAIL (401)**.

The user's request fails even though they were valid when they started.

## The Solution: Proactive Refresh with Time Buffer

We implement a **Safe Buffer** (e.g., 60 seconds). If a token is valid but has less than 60 seconds of life remaining, we treat it as "expired" for the purpose of starting a new transaction, and we refresh it **before** sending the request to the backend.

## Implementation Guide

The responsibility lies with the component holding the **Refresh Token**.

### 1. Web Application (BFF Pattern)

The BFF holds the Refresh Token in its session store (Redis).

*   **Mechanism:** A Servlet Filter (or Pre-Request Logic).
*   **Trigger:** On every incoming request to a secured API (`/api/**`).
*   **Logic:**
    1.  Extract the Session ID (JTI) from the `HttpOnly` cookie.
    2.  Load the `OAuth2AuthorizedClient` from Redis.
    3.  Check `Access Token Expiration`.
    4.  **Condition:** `if (Expiration - Now) < 60 seconds`:
        *   Execute **Refresh Token Grant** with Keycloak.
        *   Update the `OAuth2AuthorizedClient` with the new Access/Refresh tokens.
        *   Save back to Redis.
    5.  Proceed with the request using the (potentially new) Access Token.

### 2. Mobile Application

The Mobile App holds the Refresh Token in secure storage (Keychain/Keystore).

*   **Mechanism:** HTTP Client Interceptor (Retrofit, Axios, Alamofire).
*   **Trigger:** Before every network request.
*   **Logic:**
    1.  Check local Access Token `exp` claim.
    2.  **Condition:** `if (Expiration - Now) < 60 seconds`:
        *   Call Keycloak Token Endpoint (`grant_type=refresh_token`).
        *   Save new tokens to secure storage.
    3.  Attach the valid Access Token to the header.
    4.  Execute the API request.

## Sequence Diagram (BFF Example)

```mermaid
sequenceDiagram
    participant User as Browser
    participant Filter as BFF TokenFilter
    participant Redis
    participant Keycloak
    participant GW as Gateway

    User->>Filter: GET /api/orders (Cookie)
    Filter->>Redis: Load Client (JTI)
    Redis-->>Filter: Returns Client (Token expires in 30s)
    
    Note over Filter: 30s < 60s Buffer -> REFRESH NEEDED
    
    Filter->>Keycloak: Refresh Token Request
    Keycloak-->>Filter: New Access Token (Expires in 5m)
    Filter->>Redis: Update Client
    
    Filter->>GW: Forward Request (Bearer NEW_TOKEN)
    GW-->>Filter: Success
    Filter-->>User: Success
```
