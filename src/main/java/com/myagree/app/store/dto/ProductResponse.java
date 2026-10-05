package com.myagree.app.store.dto;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.Tone;
import com.myagree.app.store.ProductCategory;

/** Mirrors {@code Product} in frontend/src/lib/types.ts. */
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
        String imageUrl,
        @Nullable Double rating,
        @Nullable String imageBadge,
        Tone imageBadgeTone,
        String stockNote,
        Tone stockTone,
        String footerIcon,
        String footerText,
        Tone footerTone,
        boolean flashDeal) {
}
