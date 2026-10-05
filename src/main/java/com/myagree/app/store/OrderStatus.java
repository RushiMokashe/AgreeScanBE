package com.myagree.app.store;

/**
 * Where an order stands; mirrors {@code OrderStatus} in frontend/src/lib/types.ts. Online orders go
 * {@code AWAITING_PAYMENT → PAID}; cash-on-delivery orders are {@code PLACED} at checkout.
 */
public enum OrderStatus {
    AWAITING_PAYMENT,
    PLACED,
    PAID,
    CANCELLED
}
