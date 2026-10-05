package com.myagree.app.store.dto;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

/** Mirrors {@code StoreHome} in frontend/src/lib/types.ts. */
public record StoreHomeResponse(
        String depotName,
        boolean open,
        String deliveryLabel,
        @Nullable RxBundleResponse rxBundle,
        int rxCount,
        List<StoreCategoryResponse> categories,
        Instant flashDealEndsAt,
        List<ProductResponse> flashDeals) {
}
