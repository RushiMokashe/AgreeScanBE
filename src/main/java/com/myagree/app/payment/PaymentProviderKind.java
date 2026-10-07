package com.myagree.app.payment;

/** Which provider takes a payment; mirrors {@code PaymentProviderKind} in frontend/src/lib/types.ts. */
public enum PaymentProviderKind {
    STRIPE,
    RAZORPAY,
    /** A test provider inside AgriScan: no network and no real money. */
    SIMULATED
}
