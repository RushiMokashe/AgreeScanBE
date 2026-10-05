package com.myagree.app.account;

import com.myagree.app.account.dto.AuthSessionResponse;

/**
 * A started or renewed session.
 *
 * @param response     the response body
 * @param refreshToken the value of the refresh cookie
 */
public record SignedInSession(AuthSessionResponse response, String refreshToken) {
}
