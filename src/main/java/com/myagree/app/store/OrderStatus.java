package com.myagree.app.store;

/**
 * Where an order stands; mirrors {@code OrderStatus} in frontend/src/lib/types.ts. Online orders go
 * {@code AWAITING_PAYMENT → PAID}; cash-on-delivery orders are {@code PLACED} at checkout.
 */
public enum OrderStatus {
    /** An online order waiting for its payment. */
    AWAITING_PAYMENT,
    /** The farmer paid the shop by Scan & Pay; the shopkeeper has yet to confirm the money arrived. */
    VERIFYING_PAYMENT,
    /** A cash-on-delivery order, paid when it is delivered. */
    PLACED,
    PAID,
    CANCELLED
}
