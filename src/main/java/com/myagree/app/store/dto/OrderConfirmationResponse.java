package com.myagree.app.store.dto;

import com.myagree.app.store.OrderStatus;

/** Mirrors {@code OrderConfirmation} in frontend/src/lib/types.ts. */
public record OrderConfirmationResponse(
        long orderId,
        int itemCount,
        long total,
        OrderStatus status,
        String message) {
}
