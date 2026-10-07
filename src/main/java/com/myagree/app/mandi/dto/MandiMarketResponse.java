package com.myagree.app.mandi.dto;

/** Mirrors {@code MandiMarket} in frontend/src/lib/types.ts. */
public record MandiMarketResponse(
        long id,
        String name,
        String shortName,
        String district,
        boolean open,
        String statusLabel,
        String boardLabel,
        int arrivalsQuintals,
        String imageUrl,
        String helplinePhone,
        String helplineDisplay,
        String helplineHours) {
}
