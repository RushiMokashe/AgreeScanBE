package com.myagree.app.farmer.dto;

import org.jspecify.annotations.Nullable;

/**
 * Mirrors {@code FarmerPreferences} in frontend/src/lib/types.ts.
 *
 * @param rentalHubId   the hub chosen on the rentals screen; {@code null} for the default hub
 * @param mandiMarketId the market chosen on the mandi screen; {@code null} for the default market
 */
public record FarmerPreferencesResponse(@Nullable Long rentalHubId, @Nullable Long mandiMarketId) {
}
