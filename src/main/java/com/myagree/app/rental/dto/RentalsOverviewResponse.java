package com.myagree.app.rental.dto;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Mirrors {@code RentalsOverview} in frontend/src/lib/types.ts.
 *
 * @param onlineCount the hub's vehicles taking bookings now, the spotlight's included
 * @param spotlight   {@code null} when the hub has no spotlight offer taking bookings
 */
public record RentalsOverviewResponse(
        long hubId,
        String hubName,
        int radiusKm,
        String routes,
        long onlineCount,
        @Nullable RentalSpotlightResponse spotlight,
        List<RentalListingResponse> listings) {
}
