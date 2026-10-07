package com.myagree.app.payment;

import java.util.Set;

/** Where an online payment stands; mirrors {@code PaymentStatusCode} in frontend/src/lib/types.ts. */
public enum PaymentStatusCode {

    /** Opened at the provider; the farmer has not paid yet. */
    REQUIRES_PAYMENT,
    /** The provider is still processing it, e.g. a UPI collect request waiting for approval. */
    PROCESSING,
    /** The money was received. Final. */
    SUCCEEDED,
    /** Declined or abandoned; nothing was charged, and the farmer can pay again with a new payment. */
    FAILED,
    /** Replaced by a newer payment for the same order or booking. */
    CANCELLED;

    /** A payment the farmer can still complete, so asking to pay the same reference again returns it. */
    static final Set<PaymentStatusCode> UNFINISHED = Set.of(REQUIRES_PAYMENT, PROCESSING);
}
