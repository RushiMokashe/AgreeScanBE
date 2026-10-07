package com.myagree.app.rental.dto;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.i18n.LocalizedTextDto;
import com.myagree.app.rental.RateUnit;
import com.myagree.app.rental.RentalCategory;

/**
 * Mirrors {@code OwnerListing} in frontend/src/lib/types.ts: the owner's editable view of a vehicle.
 *
 * @param openBookings the vehicle's requested and accepted bookings
 */
public record OwnerListingResponse(
        long id,
        RentalCategory category,
        long hubId,
        LocalizedTextDto name,
        LocalizedTextDto description,
        @Nullable String imageUrl,
        @Nullable String icon,
        int rate,
        RateUnit rateUnit,
        @Nullable LocalizedTextDto rateNote,
        LocalizedTextDto availability,
        boolean online,
        List<OwnerSpecDto> specs,
        List<OwnerFeatureDto> features,
        long openBookings) {
}
