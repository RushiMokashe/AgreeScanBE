package com.myagree.app.store.dto;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.Tone;
import com.myagree.app.store.ProductCategory;

/**
 * Mirrors {@code Product} in frontend/src/lib/types.ts.
 *
 * @param imageUrl {@code null} for catalogue items without a photo, which show their category's icon tile
 * @param barcode  the EAN-13 printed on the pack
 */
public record ProductResponse(
        long id,
        String name,
        String shortName,
        ProductCategory category,
        @Nullable String tag,
        Tone tagTone,
        @Nullable String packSize,
        String description,
        int price,
        @Nullable Integer mrp,
        @Nullable String imageUrl,
        @Nullable Double rating,
        @Nullable String imageBadge,
        Tone imageBadgeTone,
        String stockNote,
        Tone stockTone,
        String footerIcon,
        String footerText,
        Tone footerTone,
        boolean flashDeal,
        boolean inStock,
        @Nullable String barcode,
        long shopId,
        String shopName) {
}
