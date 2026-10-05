package com.myagree.app.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.servlet.http.Cookie;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Requests of the sign-in flow as the frontend sends them (frontend/src/lib/api.ts). */
final class AuthRequests {

    static final String REFRESH_COOKIE = RefreshCookies.NAME;

    private AuthRequests() {
    }

    static MockHttpServletRequestBuilder login(String phone, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\": \"%s\", \"password\": \"%s\"}".formatted(phone, password));
    }

    static MockHttpServletRequestBuilder refresh(Cookie refreshCookie) {
        return post("/api/auth/refresh")
                .header(AuthController.REQUESTED_WITH_HEADER, AuthController.REQUESTED_WITH_VALUE)
                .cookie(refreshCookie);
    }

    static MockHttpServletRequestBuilder logout(Cookie refreshCookie) {
        return post("/api/auth/logout").cookie(refreshCookie);
    }

    /** The refresh cookie a sign-in, refresh or password change set. */
    static Cookie refreshCookieOf(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(REFRESH_COOKIE);
        if (cookie == null) {
            throw new AssertionError("The response set no " + REFRESH_COOKIE + " cookie");
        }
        return new Cookie(REFRESH_COOKIE, cookie.getValue());
    }
}
