package com.myagree.app.store.dto;

import java.time.Instant;
import java.util.List;

import com.myagree.app.store.OrderStatus;
import com.myagree.app.store.PaymentMethod;

/**
 * Mirrors {@code OrderSummary} in frontend/src/lib/types.ts: one of the farmer's orders with its lines.
 *
 * @param summary the products by short name, e.g. "Mancozeb 500g + Doodh Dhara 5kg"
 */
public record OrderSummaryResponse(
        long id,
        Instant placedAt,
        int itemCount,
        long total,
        OrderStatus status,
        PaymentMethod paymentMethod,
        String summary,
        List<OrderLineResponse> lines) {

    /** Mirrors {@code OrderLine}. */
    public record OrderLineResponse(String name, int quantity, int unitPrice, long lineTotal) {
    }
}
