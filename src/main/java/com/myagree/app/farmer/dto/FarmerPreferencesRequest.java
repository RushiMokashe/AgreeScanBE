package com.myagree.app.farmer.dto;

import org.jspecify.annotations.Nullable;

/**
 * Body of {@code PUT /api/farmer/me/preferences}: {@code Partial<FarmerPreferences>} in frontend/src/lib/types.ts.
 * A choice left out keeps the saved one.
 */
public record FarmerPreferencesRequest(@Nullable Long rentalHubId, @Nullable Long mandiMarketId) {
}
