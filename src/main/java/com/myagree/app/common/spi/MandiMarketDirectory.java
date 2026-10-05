package com.myagree.app.common.spi;

/**
 * Whether a mandi market exists, for slices that store a market id a user chose.
 *
 * <p><b>Implemented by</b> the market slice. <b>Consumed by</b> the farm slice, to check a farmer's preferred market
 * ({@code PUT /api/farmer/me/preferences}), through {@code ObjectProvider<MandiMarketDirectory>}; without the market
 * slice no market id is accepted.
 *
 * <p><b>Contract:</b> read-only; an unknown id gives {@code false}, never an exception.
 */
public interface MandiMarketDirectory {

    boolean exists(long marketId);
}
