package com.myagree.app.payment.dto;

import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * Body of {@code POST /api/seller/payments/{id}/not-received}, which may be empty.
 *
 * @param reason what the seller wants the farmer to know, e.g. "No money reached my account"; {@code null} for none
 */
public record NotReceivedRequest(@Size(max = NotReceivedRequest.MAX_REASON_LENGTH) @Nullable String reason) {

    public static final int MAX_REASON_LENGTH = 200;
    public static final NotReceivedRequest EMPTY = new NotReceivedRequest(null);
}
