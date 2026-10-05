package com.myagree.app.account;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.myagree.app.common.AgriScanProperties;

/**
 * The refresh-token cookie: HttpOnly so scripts cannot read it, SameSite=Strict so other sites cannot send it,
 * and scoped to {@code /api/auth} so no other endpoint receives it.
 */
@Component
class RefreshCookies {

    static final String NAME = "agriscan_refresh";

    private static final String PATH = "/api/auth";
    private static final String SAME_SITE = "Strict";

    private final boolean secure;
    private final Duration maxAge;

    RefreshCookies(AgriScanProperties properties) {
        this.secure = properties.security().cookieSecure();
        this.maxAge = properties.security().refreshTokenTtl();
    }

    /** Stores {@code refreshToken} in the browser for as long as the token lives. */
    String issue(String refreshToken) {
        return cookie(refreshToken, maxAge);
    }

    /** Removes the cookie from the browser. */
    String clear() {
        return cookie("", Duration.ZERO);
    }

    private String cookie(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(SAME_SITE)
                .path(PATH)
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
