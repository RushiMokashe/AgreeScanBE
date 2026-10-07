package com.myagree.app.payment;

/** Which provider takes a payment; mirrors {@code PaymentProviderKind} in frontend/src/lib/types.ts. */
public enum PaymentProviderKind {
    STRIPE,
    RAZORPAY,
    /** A test provider inside AgriScan: no network and no real money. */
    SIMULATED,
    /**
     * The farmer paid the seller directly, scanning their UPI QR; the seller confirms the money arrived. No gateway is
     * involved, so it is not a {@link PaymentProvider}: PaymentService settles it on the seller's word.
     */
    SCAN_AND_PAY
}
