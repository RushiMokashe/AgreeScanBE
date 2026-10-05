package com.myagree.app.scan.dto;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/** Mirrors {@code ScanSummary} in frontend/src/lib/types.ts. */
public record ScanSummaryResponse(
        long id,
        String cropLabel,
        String diseaseName,
        boolean healthy,
        int confidence,
        String imageUrl,
        Instant scannedAt,
        @Nullable String prescriptionShort,
        @Nullable String note,
        int stockistCount,
        @Nullable Long plotId) {
}
