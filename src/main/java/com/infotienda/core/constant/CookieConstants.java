package com.infotienda.core.constant;

public final class CookieConstants {
    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    public static final String GUEST_SESSION_COOKIE_NAME = "guest_session_id";
    public static final int GUEST_SESSION_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

    private CookieConstants() {
    }
}
