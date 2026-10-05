package com.myagree.app.care.dto;

import com.myagree.app.care.DealerCertification;

/** Mirrors {@code AgroDealer} in frontend/src/lib/types.ts. */
public record AgroDealerResponse(
        long id,
        String name,
        DealerCertification certification,
        double distanceKm,
        String address,
        double rating,
        int reviewCount,
        String phone,
        DealerStockResponse stock) {
}
