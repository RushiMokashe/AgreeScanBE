package com.myagree.app.store.dto;

import org.jspecify.annotations.Nullable;

import com.myagree.app.store.ProductCategory;

/** Mirrors {@code AdminProduct} in frontend/src/lib/types.ts. */
public record AdminProductResponse(
        long id,
        String name,
        ProductCategory category,
        int price,
        @Nullable Integer mrp,
        boolean flashDeal,
        boolean inStock,
        @Nullable String imageUrl,
        @Nullable String barcode,
        String shopName) {
}
