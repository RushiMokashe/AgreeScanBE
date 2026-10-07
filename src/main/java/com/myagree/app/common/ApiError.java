package com.myagree.app.common;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Body of every failed API call; mirrors {@code ApiErrorBody} in frontend/src/lib/types.ts.
 *
 * @param status  HTTP status code, e.g. 404
 * @param error   HTTP reason phrase, e.g. "Not Found"
 * @param message explanation that is safe to show to the farmer, e.g. "Scan 99 not found"
 * @param code    the message's key for a client that reacts to one error in particular, e.g. "store.cart.other-shop";
 *                {@code null} for errors without one
 */
public record ApiError(int status, String error, String message, @Nullable String code) {

    public static ApiError of(HttpStatusCode status, String message) {
        return of(status, message, null);
    }

    public static ApiError of(HttpStatusCode status, String message, @Nullable String code) {
        return new ApiError(status.value(), HttpStatus.valueOf(status.value()).getReasonPhrase(), message, code);
    }
}
