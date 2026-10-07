package com.myagree.app.common.security;

import org.jspecify.annotations.Nullable;

/**
 * Sign-in for the installed Android app. Its web view loads the app from {@code https://localhost} and calls the API
 * on another origin, where the browser's {@code SameSite=Strict} refresh cookie is never sent. So the app announces
 * itself with {@link #CLIENT_HEADER}, receives the refresh token in the {@link #REFRESH_TOKEN_HEADER} response header
 * instead of a cookie, keeps it in the app's private storage, and sends it back in the same request header. Browsers
 * never add these headers by themselves, so a malicious page cannot use them the way it could use a cookie.
 */
public final class NativeClient {

    /** Sent by the app on every sign-in call; the only value is {@link #NATIVE}. */
    public static final String CLIENT_HEADER = "X-AgriScan-Client";
    public static final String NATIVE = "native";
    /**
     * The refresh token: in the response after signing in, refreshing or changing the password; in the request to
     * refresh or sign out.
     */
    public static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";

    private NativeClient() {
    }

    public static boolean isNative(@Nullable String clientHeader) {
        return NATIVE.equals(clientHeader);
    }
}
