package com.myagree.app.account.dto;

import java.time.Instant;

/**
 * Mirrors {@code AuthSession} in frontend/src/lib/types.ts. The refresh token travels only in its HttpOnly cookie.
 *
 * @param accessToken bearer token for every other API call
 * @param expiresAt   when the access token stops being accepted
 */
public record AuthSessionResponse(String accessToken, Instant expiresAt, CurrentUserResponse user) {
}
