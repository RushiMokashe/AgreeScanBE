package com.myagree.app.account;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.account.dto.AuthSessionResponse;
import com.myagree.app.account.dto.ChangePasswordRequest;
import com.myagree.app.account.dto.CurrentUserResponse;
import com.myagree.app.account.dto.LoginRequest;
import com.myagree.app.account.dto.UpdateProfileRequest;
import com.myagree.app.common.ApiError;
import com.myagree.app.common.ForbiddenException;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.Messages;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.common.security.NativeClient;

/**
 * Signing in, refreshing and signing out. Browsers keep the refresh token in an HttpOnly cookie; the installed app,
 * which announces itself with {@link NativeClient#CLIENT_HEADER}, keeps it itself and exchanges it in the
 * {@link NativeClient#REFRESH_TOKEN_HEADER} header instead.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    /** A header cross-site forms cannot set, demanded as a second guard for the cookie-authenticated refresh. */
    static final String REQUESTED_WITH_HEADER = "X-Requested-With";
    static final String REQUESTED_WITH_VALUE = "agriscan";

    private static final UserMessage REFRESH_HEADER_REQUIRED =
            UserMessage.of("common.auth.refresh-header-required", REQUESTED_WITH_HEADER, REQUESTED_WITH_VALUE);

    private final AuthService authService;
    private final RefreshCookies refreshCookies;
    private final Messages messages;

    AuthController(AuthService authService, RefreshCookies refreshCookies, Messages messages) {
        this.authService = authService;
        this.refreshCookies = refreshCookies;
        this.messages = messages;
    }

    @PostMapping("/login")
    ResponseEntity<AuthSessionResponse> login(
            @RequestHeader(name = NativeClient.CLIENT_HEADER, required = false) @Nullable String client,
            @Valid @RequestBody LoginRequest request) {
        return signedIn(authService.login(request.phone(), request.password()), NativeClient.isNative(client));
    }

    /**
     * Trades the refresh token for a new session. The app sends it in a header; a browser sends its cookie, plus a
     * header cross-site forms cannot set.
     */
    @PostMapping("/refresh")
    ResponseEntity<AuthSessionResponse> refresh(
            @RequestHeader(name = NativeClient.CLIENT_HEADER, required = false) @Nullable String client,
            @RequestHeader(name = NativeClient.REFRESH_TOKEN_HEADER, required = false) @Nullable String headerToken,
            @RequestHeader(name = REQUESTED_WITH_HEADER, required = false) @Nullable String requestedWith,
            @CookieValue(name = RefreshCookies.NAME, required = false) @Nullable String cookieToken) {
        if (NativeClient.isNative(client)) {
            return signedIn(authService.refresh(headerToken), true);
        }
        if (!REQUESTED_WITH_VALUE.equals(requestedWith)) {
            throw new ForbiddenException(REFRESH_HEADER_REQUIRED);
        }
        return signedIn(authService.refresh(cookieToken), false);
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @RequestHeader(name = NativeClient.CLIENT_HEADER, required = false) @Nullable String client,
            @RequestHeader(name = NativeClient.REFRESH_TOKEN_HEADER, required = false) @Nullable String headerToken,
            @CookieValue(name = RefreshCookies.NAME, required = false) @Nullable String cookieToken) {
        if (NativeClient.isNative(client)) {
            authService.logout(headerToken);
            return ResponseEntity.noContent().build();
        }
        authService.logout(cookieToken);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookies.clear()).build();
    }

    @GetMapping("/me")
    CurrentUserResponse currentUser(CurrentUser user) {
        return authService.currentUser(user.userId());
    }

    @PatchMapping("/me")
    CurrentUserResponse updateProfile(CurrentUser user, @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(user.userId(), request);
    }

    /** Signs every other device out; this device keeps its session through a new refresh token. */
    @PostMapping("/me/password")
    ResponseEntity<Void> changePassword(
            CurrentUser user,
            @RequestHeader(name = NativeClient.CLIENT_HEADER, required = false) @Nullable String client,
            @Valid @RequestBody ChangePasswordRequest request) {
        String refreshToken = authService.changePassword(user.userId(), request);
        return ResponseEntity.noContent()
                .headers(refreshTokenHeaders(refreshToken, NativeClient.isNative(client)))
                .build();
    }

    /** A refresh cookie that can no longer renew the session is removed from the browser. */
    @ExceptionHandler(InvalidSessionException.class)
    ResponseEntity<ApiError> sessionEnded(InvalidSessionException ex, Language language) {
        return ResponseEntity.status(ex.status())
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear())
                .body(ApiError.of(ex.status(), ex.userMessage(messages, language)));
    }

    private ResponseEntity<AuthSessionResponse> signedIn(SignedInSession session, boolean nativeClient) {
        return ResponseEntity.ok()
                .headers(refreshTokenHeaders(session.refreshToken(), nativeClient))
                .body(session.response());
    }

    /** Hands {@code refreshToken} to the client: in a header for the app, in an HttpOnly cookie for a browser. */
    private HttpHeaders refreshTokenHeaders(String refreshToken, boolean nativeClient) {
        HttpHeaders headers = new HttpHeaders();
        if (nativeClient) {
            headers.set(NativeClient.REFRESH_TOKEN_HEADER, refreshToken);
        } else {
            headers.set(HttpHeaders.SET_COOKIE, refreshCookies.issue(refreshToken));
        }
        return headers;
    }
}
