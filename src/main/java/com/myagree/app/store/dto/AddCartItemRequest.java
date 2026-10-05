package com.myagree.app.store.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Body of {@code POST /api/cart/items}. */
public record AddCartItemRequest(
        @NotNull Long productId,
        @NotNull @Positive @Max(AddCartItemRequest.MAX_QUANTITY_PER_ADD) Integer quantity) {

    static final int MAX_QUANTITY_PER_ADD = 99;
}
