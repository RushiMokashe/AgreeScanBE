package com.myagree.app.mandi;

import java.util.List;

import com.myagree.app.mandi.dto.BuyerInquiryResponse;
import com.myagree.app.mandi.dto.CommodityPriceResponse;
import com.myagree.app.mandi.dto.MandiMarketResponse;
import com.myagree.app.mandi.dto.MandiOverviewResponse;
import com.myagree.app.mandi.dto.PriceForecastResponse;

final class MandiMapper {

    private MandiMapper() {
    }

    static MandiOverviewResponse toOverview(MandiMarket market, List<CommodityPrice> prices, List<BuyerInquiry> inquiries) {
        PriceForecast forecast = market.getForecast();
        return new MandiOverviewResponse(
                toResponse(market),
                new PriceForecastResponse(forecast.tag(), forecast.message()),
                market.getPricesUpdatedAt(),
                prices.stream().map(MandiMapper::toResponse).toList(),
                inquiries.stream().map(MandiMapper::toResponse).toList());
    }

    static BuyerInquiryResponse toResponse(BuyerInquiry inquiry) {
        return new BuyerInquiryResponse(
                inquiry.getId(),
                inquiry.getBuyerName(),
                inquiry.isVerified(),
                inquiry.getSubtitle(),
                inquiry.getIcon(),
                inquiry.getBadge(),
                inquiry.isBadgeHighlighted(),
                inquiry.getVolumeTonnes(),
                inquiry.getProduce(),
                inquiry.getPriceLabel(),
                inquiry.getPricePerQuintal(),
                inquiry.getPriceNote(),
                inquiry.isPriceNoteHighlighted(),
                inquiry.isHighlighted(),
                inquiry.getSpecsTitle(),
                inquiry.getSpecs(),
                inquiry.getFooterIcon(),
                inquiry.getFooterText(),
                inquiry.getActionLabel(),
                inquiry.getActionIcon(),
                inquiry.isActionPrimary(),
                inquiry.isResponded());
    }

    private static MandiMarketResponse toResponse(MandiMarket market) {
        MandiHelpline helpline = market.getHelpline();
        return new MandiMarketResponse(
                market.getId(),
                market.getName(),
                market.getShortName(),
                market.isOpen(),
                market.getStatusLabel(),
                market.getBoardLabel(),
                market.getArrivalsQuintals(),
                market.getImageUrl(),
                helpline.phone(),
                helpline.displayNumber(),
                helpline.hours());
    }

    private static CommodityPriceResponse toResponse(CommodityPrice price) {
        return new CommodityPriceResponse(
                price.getId(),
                price.getCategory(),
                price.getName(),
                price.getGrade(),
                price.getMinPrice(),
                price.getMaxPrice(),
                price.getUnit(),
                price.getChangeAmount(),
                price.getDemand());
    }
}
