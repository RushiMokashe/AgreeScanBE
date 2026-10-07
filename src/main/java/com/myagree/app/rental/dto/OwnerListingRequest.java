package com.myagree.app.rental.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.i18n.LocalizedTextDto;
import com.myagree.app.rental.RateUnit;
import com.myagree.app.rental.RentalCategory;
import com.myagree.app.rental.RentalListing;

/**
 * Body of {@code POST /api/owner/listings} and {@code PUT /api/owner/listings/{id}}; mirrors {@code OwnerListingInput}
 * in frontend/src/lib/types.ts.
 *
 * @param icon the Material Symbol of a card without a photo; {@code null} for the category's default
 */
public record OwnerListingRequest(
        @NotNull RentalCategory category,
        @NotNull Long hubId,
        @NotNull @Valid LocalizedTextDto name,
        @NotNull @Valid LocalizedTextDto description,
        @Pattern(regexp = OwnerListingRequest.ICON_PATTERN) @Nullable String icon,
        @Min(1) @Max(RentalListing.MAX_RATE) int rate,
        @NotNull RateUnit rateUnit,
        @Valid @Nullable LocalizedTextDto rateNote,
        @NotNull @Valid LocalizedTextDto availability,
        boolean online,
        @NotNull @Size(max = RentalListing.MAX_SPECS) List<@NotNull @Valid OwnerSpecDto> specs,
        @NotNull @Size(max = RentalListing.MAX_FEATURES) List<@NotNull @Valid OwnerFeatureDto> features) {

    /** A Material Symbols name, e.g. "local_shipping". */
    public static final String ICON_PATTERN = "[a-z0-9_]{1,40}";
}
