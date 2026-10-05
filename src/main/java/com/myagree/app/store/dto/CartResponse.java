package com.myagree.app.store.dto;

import java.util.List;

/** Mirrors {@code Cart} in frontend/src/lib/types.ts. */
public record CartResponse(
        List<CartItemResponse> items,
        int itemCount,
        long total,
        boolean freeDelivery,
        String summary) {
}
