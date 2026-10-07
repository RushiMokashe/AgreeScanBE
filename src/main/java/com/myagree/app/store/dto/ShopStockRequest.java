package com.myagree.app.store.dto;

import jakarta.validation.constraints.NotNull;

/** Body of {@code PATCH /api/shop/products/{id}/stock}. */
public record ShopStockRequest(@NotNull Boolean inStock) {
}
