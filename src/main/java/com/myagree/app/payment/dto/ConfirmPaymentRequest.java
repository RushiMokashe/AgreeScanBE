package com.myagree.app.payment.dto;

import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * Body of {@code POST /api/payments/{id}/confirm}; mirrors {@code ConfirmPaymentRequest} in frontend/src/lib/types.ts.
 * Stripe confirmations need no body: the server asks Stripe. Razorpay sends what its checkout returned, the simulated
 * provider the outcome the tester chose.
 */
public record ConfirmPaymentRequest(
        @Size(max = 64) @Nullable String razorpayPaymentId,
        @Size(max = 64) @Nullable String razorpayOrderId,
        @Size(max = 128) @Nullable String razorpaySignature,
        @Nullable SimulatedOutcome simulatedOutcome,
        @Nullable SimulatedMethod simulatedMethod) {

    public static final ConfirmPaymentRequest EMPTY = new ConfirmPaymentRequest(null, null, null, null, null);

    /** How a simulated payment ends. */
    public enum SimulatedOutcome {
        SUCCEEDED,
        FAILED
    }

    /** What the tester pretended to pay with. */
    public enum SimulatedMethod {
        CARD,
        UPI
    }
}
