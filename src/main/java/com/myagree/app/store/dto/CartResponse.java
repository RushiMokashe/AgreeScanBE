package com.myagree.app.store.dto;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Mirrors {@code Cart} in frontend/src/lib/types.ts.
 *
 * @param shopId   the one shop whose products the cart holds; {@code null} when it is empty
 * @param shopName that shop's name
 */
public record CartResponse(
        List<CartItemResponse> items,
        int itemCount,
        long total,
        boolean freeDelivery,
        String summary,
        @Nullable Long shopId,
        @Nullable String shopName) {
}
