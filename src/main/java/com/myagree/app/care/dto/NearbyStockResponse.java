package com.myagree.app.care.dto;

import java.util.List;

import org.jspecify.annotations.Nullable;

/** Mirrors {@code NearbyStock} in frontend/src/lib/types.ts. */
public record NearbyStockResponse(
        String hubName,
        String district,
        int radiusKm,
        String productsLabel,
        String mapImageUrl,
        @Nullable OnlineOfferResponse onlineOffer,
        List<AgroDealerResponse> dealers) {
}
