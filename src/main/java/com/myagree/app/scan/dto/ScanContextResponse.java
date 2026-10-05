package com.myagree.app.scan.dto;

import java.util.List;

import org.jspecify.annotations.Nullable;

/** Mirrors {@code ScanContext} in frontend/src/lib/types.ts. */
public record ScanContextResponse(
        String modelVersion,
        List<CropOptionResponse> supportedCrops,
        int fieldMoisturePercent,
        String fieldMoistureNote,
        int nearbyOutbreakCount,
        String nearbyOutbreakNote,
        @Nullable ScanDetailResponse latestScan) {
}
