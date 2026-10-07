package com.myagree.app.store.dto;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Body of {@code POST /api/cart/items}. */
public record AddCartItemRequest(
        @NotNull Long productId,
        @NotNull @Positive @Max(AddCartItemRequest.MAX_QUANTITY_PER_ADD) Integer quantity,
        @Nullable Boolean replaceCart) {

    /** Whether to empty a cart holding another shop's products first, as the farmer agreed. */
    public boolean replacesCart() {
        return Boolean.TRUE.equals(replaceCart);
    }

    static final int MAX_QUANTITY_PER_ADD = 99;
}
