package com.myagree.app.seed;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

import com.myagree.app.mandi.BuyerInquiry;
import com.myagree.app.mandi.BuyerInquiryRepository;
import com.myagree.app.mandi.CommodityPrice;
import com.myagree.app.mandi.CommodityPriceRepository;
import com.myagree.app.mandi.DemandLevel;
import com.myagree.app.mandi.MandiHelpline;
import com.myagree.app.mandi.MandiMarket;
import com.myagree.app.mandi.MandiMarketRepository;
import com.myagree.app.mandi.PriceForecast;

/** Solapur APMC's live board, commodity rates and buyer inquiries from the mandi screen. */
@Component
class MandiSeed implements DemoSeed {

    /** "Updated 18 min ago" in the design. */
    private static final Duration PRICES_UPDATED_AGO = Duration.ofMinutes(18);
    private static final String QUINTAL = "Quintal";

    private final MandiMarketRepository marketRepository;
    private final CommodityPriceRepository priceRepository;
    private final BuyerInquiryRepository inquiryRepository;
    private final Clock clock;

    MandiSeed(MandiMarketRepository marketRepository, CommodityPriceRepository priceRepository,
              BuyerInquiryRepository inquiryRepository, Clock clock) {
        this.marketRepository = marketRepository;
        this.priceRepository = priceRepository;
        this.inquiryRepository = inquiryRepository;
        this.clock = clock;
    }

    @Override
    public int order() {
        return 80;
    }

    @Override
    public void seed(SeedContext context) {
        MandiMarket market = marketRepository.save(new MandiMarket(
                "Solapur APMC Mandi", "Solapur Main APMC", true,
                "Market Open • Trading Active", "Live Wholesale Board • आजचे बाजारभाव",
                8450, "/images/mandi/solapur-apmc.jpg",
                new MandiHelpline("18002331020", "1800-233-1020", "6 AM - 8 PM"),
                new PriceForecast("Festive Surge", "Tomato wholesale rates projected to rise **₹150–220/Qtl** over next "
                        + "72 hrs. Recommended sale window: **Tomorrow to Thursday**."),
                clock.instant().minus(PRICES_UPDATED_AGO)));
        priceRepository.saveAll(List.of(
                new CommodityPrice(market, "Vegetable", "Tomato (Hybrid)", "Grade A (Crates)", 1850, 2400, QUINTAL, 180, DemandLevel.HIGH_DEMAND),
                new CommodityPrice(market, "Bulb", "Red Onion (Garwa)", "Solapur Medium", 2100, 2750, QUINTAL, 0, DemandLevel.STABLE),
                new CommodityPrice(market, "Cereal", "Wheat (Lokwan)", "Dry Hard Grain", 2800, 3100, QUINTAL, 40, DemandLevel.STEADY),
                new CommodityPrice(market, "Spices", "Green Chilli (G-4)", "Dark Green Crisp", 3400, 3950, QUINTAL, 420, DemandLevel.HOT)));
        inquiryRepository.saveAll(List.of(retailInquiry(market), exportInquiry(market), processingInquiry(market)));
    }

    private static BuyerInquiry retailInquiry(MandiMarket market) {
        return BuyerInquiry.builder(market, "Reliance Retail & BigBasket", "Verified Institutional Sourcing Partner")
                .verified()
                .icon("storefront")
                .badge("Spot Settlement", true)
                .requirement(15, "Table Tomatoes")
                .price("Offered Price", 2450, "Direct DBT in 24 Hrs", false)
                .highlighted()
                .specs("Grade A Spec Requirements:",
                        List.of("55-65 mm Caliber", "80% Color Break (Firm Red)", "Zero Fruit Borer / Scars"))
                .footer("pin_drop", "APMC Gate 3 • Pickup available")
                .action("Sell Harvest", "arrow_forward", true)
                .build();
    }

    private static BuyerInquiry exportInquiry(MandiMarket market) {
        return BuyerInquiry.builder(market, "Sahyadri FPC Export Div.", "Gulf & SE Asia Export Program")
                .verified()
                .icon("public")
                .badge("+₹200 Premium", true)
                .requirement(25, "Export Red Onions")
                .price("Purchase Price", 2800, "Bonus Grade Incentive", true)
                .highlighted()
                .specs("Export Sorting Guidelines:",
                        List.of("50mm+ Tight Bulb", "Thin Dry Neck", "Double Intact Red Skin"))
                .footer("schedule", "Closes tomorrow 4:00 PM")
                .action("Submit Lot Details", "upload_file", false)
                .build();
    }

    private static BuyerInquiry processingInquiry(MandiMarket market) {
        return BuyerInquiry.builder(market, "Kissan Puree & Processing", "Bulk Processing Supply Plant")
                .icon("factory")
                .badge("Grade B Accepted", false)
                .requirement(40, "Ripe Processing Crop")
                .price("Contract Price", 1650, "Immediate unloading", false)
                .footer("scale", "Min. Brix Sweetness 4.5°")
                .action("View Lot Tender", null, false)
                .build();
    }
}
