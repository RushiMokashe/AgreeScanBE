package com.myagree.app.store.dto;

import com.myagree.app.store.OrderStatus;
import com.myagree.app.store.PaymentMethod;

/** Mirrors {@code OrderConfirmation} in frontend/src/lib/types.ts. */
public record OrderConfirmationResponse(
        long orderId,
        int itemCount,
        long total,
        OrderStatus status,
        PaymentMethod paymentMethod,
        String message) {
}
