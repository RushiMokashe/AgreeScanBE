package com.myagree.app.common.spi;

import java.util.Optional;

/**
 * A farmer's saved choices, for the screens that open on them.
 *
 * <p><b>Implemented by</b> the farm slice, which saves them with the farmer profile
 * ({@code PUT /api/farmer/me/preferences}). <b>Consumed by</b> the rental slice ({@code GET /api/rentals} without
 * {@code hubId}) and the market slice ({@code GET /api/mandi} without {@code marketId}) through
 * {@code ObjectProvider<FarmerPreferencesReader>}; without the farm slice, or without a saved choice, they open on
 * their first hub or market.
 *
 * <p><b>Contract:</b> read-only; empty when nothing is saved or the farmer is unknown, never an exception. A saved hub
 * or market may have been removed since, so consumers check that it still exists.
 */
public interface FarmerPreferencesReader {

    Optional<Long> preferredRentalHub(long farmerId);

    Optional<Long> preferredMandiMarket(long farmerId);
}
