package com.myagree.app.rental.dto;

import org.jspecify.annotations.Nullable;

import com.myagree.app.rental.RateUnit;
import com.myagree.app.rental.RentalCategory;

/**
 * Mirrors {@code AdminRentalListing} in frontend/src/lib/types.ts.
 *
 * @param ownerPhone the owner's 10-digit mobile number
 */
public record AdminRentalListingResponse(
        long id,
        String name,
        RentalCategory category,
        String hubName,
        String ownerName,
        String ownerPhone,
        int rate,
        RateUnit rateUnit,
        boolean online,
        @Nullable String imageUrl,
        @Nullable String icon) {
}
