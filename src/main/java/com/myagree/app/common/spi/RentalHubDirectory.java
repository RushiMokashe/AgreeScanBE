package com.myagree.app.common.spi;

/**
 * Whether a rental hub exists, for slices that store a hub id a user chose.
 *
 * <p><b>Implemented by</b> the rental slice. <b>Consumed by</b> the farm slice, to check a farmer's preferred hub
 * ({@code PUT /api/farmer/me/preferences}), through {@code ObjectProvider<RentalHubDirectory>}; without the rental
 * slice no hub id is accepted.
 *
 * <p><b>Contract:</b> read-only; an unknown id gives {@code false}, never an exception.
 */
public interface RentalHubDirectory {

    boolean exists(long hubId);
}
