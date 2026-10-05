package com.myagree.app.mandi.dto;

import com.myagree.app.mandi.DemandLevel;

/** Mirrors {@code CommodityPrice} in frontend/src/lib/types.ts. */
public record CommodityPriceResponse(
        long id,
        String category,
        String name,
        String grade,
        int minPrice,
        int maxPrice,
        String unit,
        int changeAmount,
        DemandLevel demand) {
}
