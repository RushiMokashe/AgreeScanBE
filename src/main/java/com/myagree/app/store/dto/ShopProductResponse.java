package com.myagree.app.store.dto;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.i18n.LocalizedTextDto;
import com.myagree.app.store.ProductCategory;

/**
 * One of the shop's products with its texts in every language, as the shop portal edits it; mirrors
 * {@code ShopProduct} in frontend/src/lib/types.ts.
 */
public record ShopProductResponse(
        long id,
        LocalizedTextDto name,
        ProductCategory category,
        @Nullable LocalizedTextDto packSize,
        LocalizedTextDto description,
        int price,
        @Nullable Integer mrp,
        boolean inStock,
        @Nullable String barcode,
        @Nullable String imageUrl) {
}
