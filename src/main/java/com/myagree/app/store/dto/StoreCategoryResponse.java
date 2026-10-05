package com.myagree.app.store.dto;

import com.myagree.app.store.ProductCategory;

/** Mirrors {@code StoreCategory} in frontend/src/lib/types.ts. */
public record StoreCategoryResponse(
        ProductCategory category,
        String title,
        String localTitle,
        String description,
        String icon,
        String badge) {
}
