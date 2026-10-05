package com.myagree.app.rental;

/**
 * Whether a booking has been paid online; mirrors {@code BookingPaymentStatus} in frontend/src/lib/types.ts. A farmer
 * who settles with the owner after the job ("Post-Sale Payment Option") leaves it {@link #UNPAID}.
 */
public enum BookingPaymentStatus {
    UNPAID,
    PAID
}
