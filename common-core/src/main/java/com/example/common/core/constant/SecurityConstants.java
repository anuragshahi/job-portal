package com.example.common.core.constant;

public final class SecurityConstants {

    private SecurityConstants() {
        // Private constructor to prevent instantiation
    }

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";
    
    // Header constants
    public static final String HEADER_X_REQUESTED_WITH = "X-Requested-With";
    public static final String XML_HTTP_REQUEST = "XMLHttpRequest";
}
