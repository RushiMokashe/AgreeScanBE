package com.myagree.app.mandi.dto;

import java.time.Instant;
import java.util.List;

/** Mirrors {@code MandiOverview} in frontend/src/lib/types.ts. */
public record MandiOverviewResponse(
        MandiMarketResponse market,
        PriceForecastResponse forecast,
        Instant pricesUpdatedAt,
        List<CommodityPriceResponse> commodities,
        List<BuyerInquiryResponse> inquiries) {
}
