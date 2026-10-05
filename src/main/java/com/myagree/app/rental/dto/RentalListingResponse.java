package com.myagree.app.rental.dto;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.myagree.app.rental.RateUnit;
import com.myagree.app.rental.RentalCategory;

/** Mirrors {@code RentalListing} in frontend/src/lib/types.ts. */
public record RentalListingResponse(
        long id,
        RentalCategory category,
        String name,
        String description,
        @Nullable String imageUrl,
        @Nullable String icon,
        @Nullable String badge,
        @Nullable String badgeIcon,
        @Nullable Double distanceKm,
        @Nullable String locality,
        int rate,
        RateUnit rateUnit,
        @Nullable String rateNote,
        boolean rateNoteHighlighted,
        RentalOperatorResponse operator,
        String availability,
        boolean availabilityHighlighted,
        List<RentalSpecResponse> specs,
        List<RentalFeatureResponse> features,
        String phone,
        String callLabel,
        String bookLabel,
        String bookIcon,
        boolean favorite) {
}
