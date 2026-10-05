package com.myagree.app.rental.dto;

import java.util.List;

/** Mirrors {@code RentalSpotlight} in frontend/src/lib/types.ts. */
public record RentalSpotlightResponse(
        long listingId,
        String title,
        String subtitle,
        String label,
        String imageUrl,
        int baseFare,
        int perKmRate,
        int dispatchMinutes,
        List<RentalPerkResponse> perks) {
}
