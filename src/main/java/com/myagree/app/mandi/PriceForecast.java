package com.myagree.app.mandi;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * AgriScan's short-term price outlook for a market.
 *
 * @param message may contain **bold** markers
 */
@Embeddable
public record PriceForecast(
        @Column(name = "forecast_tag") String tag,
        @Column(name = "forecast_message", length = 500) String message) {
}
