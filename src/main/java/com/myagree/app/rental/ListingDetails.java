package com.myagree.app.rental;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.i18n.LocalizedText;

/**
 * What an owner says about a vehicle when listing or editing it ({@code OwnerListingInput} in
 * frontend/src/lib/types.ts), with the hub already looked up.
 *
 * @param icon the Material Symbol of the card without a photo; {@code null} for the category's default
 */
record ListingDetails(
        RentalCategory category,
        RentalHub hub,
        LocalizedText name,
        LocalizedText description,
        @Nullable String icon,
        int rate,
        RateUnit rateUnit,
        @Nullable LocalizedText rateNote,
        LocalizedText availability,
        boolean online,
        List<RentalSpec> specs,
        List<RentalFeature> features) {

    ListingDetails {
        specs = List.copyOf(specs);
        features = List.copyOf(features);
    }
}
