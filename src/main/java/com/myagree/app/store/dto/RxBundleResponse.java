package com.myagree.app.store.dto;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/** Mirrors {@code RxBundle} in frontend/src/lib/types.ts. */
public record RxBundleResponse(
        long productId,
        @Nullable Long scanId,
        String label,
        Instant prescribedAt,
        String name,
        String description,
        int price,
        int mrp,
        String imageUrl,
        String genuineLabel,
        String subsidyLabel) {
}
