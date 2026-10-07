package com.myagree.app.store.dto;

import java.time.Instant;
import java.util.List;

import com.myagree.app.store.OrderStatus;
import com.myagree.app.store.PaymentMethod;
import com.myagree.app.store.dto.OrderSummaryResponse.OrderLineResponse;

/**
 * An order placed with the shop, with the customer to deliver to; mirrors {@code ShopOrder} in
 * frontend/src/lib/types.ts.
 */
public record ShopOrderResponse(
        long id,
        Instant placedAt,
        String customerName,
        String customerPhone,
        int itemCount,
        long total,
        OrderStatus status,
        PaymentMethod paymentMethod,
        String summary,
        List<OrderLineResponse> lines) {
}
