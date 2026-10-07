package com.myagree.app.mandi;

import java.util.List;

import org.springframework.stereotype.Component;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.Messages;
import com.myagree.app.mandi.dto.BuyerInquiryResponse;
import com.myagree.app.mandi.dto.CommodityPriceResponse;
import com.myagree.app.mandi.dto.MandiMarketResponse;
import com.myagree.app.mandi.dto.MandiOverviewResponse;
import com.myagree.app.mandi.dto.PriceForecastResponse;

/** Shows the mandi board in the reader's language; the status label follows whether trading is open. */
@Component
class MandiMapper {

    private static final String STATUS_OPEN = "market.mandi.status-open";
    private static final String STATUS_CLOSED = "market.mandi.status-closed";

    private final Messages messages;

    MandiMapper(Messages messages) {
        this.messages = messages;
    }

    MandiOverviewResponse toOverview(MandiMarket market, List<CommodityPrice> prices, List<BuyerInquiry> inquiries,
                                     Language language) {
        PriceForecast forecast = market.getForecast();
        return new MandiOverviewResponse(
                toResponse(market, language),
                new PriceForecastResponse(forecast.tag(), forecast.message()),
                market.getPricesUpdatedAt(),
                prices.stream().map(price -> toResponse(price, language)).toList(),
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

    private MandiMarketResponse toResponse(MandiMarket market, Language language) {
        MandiHelpline helpline = market.getHelpline();
        return new MandiMarketResponse(
                market.getId(),
                market.getName().resolve(language),
                market.getShortName().resolve(language),
                market.getDistrict().resolve(language),
                market.isOpen(),
                messages.get(market.isOpen() ? STATUS_OPEN : STATUS_CLOSED, language),
                market.getBoardLabel().resolve(language),
                market.getArrivalsQuintals(),
                market.getImageUrl(),
                helpline.phone(),
                helpline.displayNumber(),
                helpline.hours().resolve(language));
    }

    private static CommodityPriceResponse toResponse(CommodityPrice price, Language language) {
        Commodity commodity = price.getCommodity();
        return new CommodityPriceResponse(
                price.getId(),
                commodity.category().resolve(language),
                commodity.name().resolve(language),
                commodity.grade().resolve(language),
                price.getMinPrice(),
                price.getMaxPrice(),
                commodity.unit().resolve(language),
                price.getChangeAmount(),
                price.getDemand());
    }
}
