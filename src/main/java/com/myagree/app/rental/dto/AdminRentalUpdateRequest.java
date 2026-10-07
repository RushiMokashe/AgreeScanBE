package com.myagree.app.rental.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.jspecify.annotations.Nullable;

import com.myagree.app.rental.RateUnit;
import com.myagree.app.rental.RentalListing;

/** Body of {@code PATCH /api/admin/rentals/{id}}: each field given replaces the listing's; the rest stay. */
public record AdminRentalUpdateRequest(
        @Min(1) @Max(RentalListing.MAX_RATE) @Nullable Integer rate,
        @Nullable RateUnit rateUnit,
        @Nullable Boolean online) {
}
