package com.myagree.app.store;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.i18n.LocalizedText;

/**
 * What a shopkeeper enters for one of their products, checked by {@link ShopProductService}: price within
 * {@link Product#MAX_PRICE} and not above the MRP, and a valid, unused EAN-13 barcode.
 */
record ShopListing(
        LocalizedText name,
        ProductCategory category,
        @Nullable LocalizedText packSize,
        LocalizedText description,
        int price,
        @Nullable Integer mrp,
        boolean inStock,
        @Nullable String barcode) {
}
