package com.myagree.app.rental.dto;

import java.util.List;

/** Mirrors {@code RentalsOverview} in frontend/src/lib/types.ts. */
public record RentalsOverviewResponse(
        String hubName,
        int radiusKm,
        String routes,
        int onlineCount,
        RentalSpotlightResponse spotlight,
        List<RentalListingResponse> listings) {
}
