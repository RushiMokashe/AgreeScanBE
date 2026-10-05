package com.myagree.app.common.security;

import java.time.Instant;

/**
 * A signed access token, sent as {@code Authorization: Bearer <value>}.
 *
 * @param value     the compact JWT
 * @param expiresAt when the token stops being accepted
 */
public record AccessToken(String value, Instant expiresAt) {
}
