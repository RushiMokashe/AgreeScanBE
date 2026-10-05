package com.myagree.app.store;

/** How the farmer pays for an order; mirrors {@code PaymentMethod} in frontend/src/lib/types.ts. */
public enum PaymentMethod {

    /** Paid through the payment slice right after checkout; the order waits for the payment. */
    ONLINE,

    /** Paid in cash when the depot delivers; the order is placed right away. */
    CASH_ON_DELIVERY
}
