package com.myagree.app.rental;

/**
 * Whether a booking has been paid online; mirrors {@code BookingPaymentStatus} in frontend/src/lib/types.ts. A farmer
 * who settles with the owner after the job ("Post-Sale Payment Option") leaves it {@link #UNPAID}.
 */
public enum BookingPaymentStatus {
    UNPAID,
    /** The farmer paid the owner by Scan & Pay; the owner has yet to confirm the money arrived. */
    VERIFYING,
    PAID
}
