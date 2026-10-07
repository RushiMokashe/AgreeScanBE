package com.myagree.app.rental.dto;

import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/** Mirrors {@code BookingReasonRequest} in frontend/src/lib/types.ts: why a booking is declined or cancelled. */
public record BookingReasonRequest(@Size(max = BookingReasonRequest.MAX_REASON_LENGTH) @Nullable String reason) {

    public static final int MAX_REASON_LENGTH = 200;
}
