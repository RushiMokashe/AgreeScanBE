package com.myagree.app.mandi;

import java.time.Instant;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.EmbeddedColumnNaming;

import com.myagree.app.common.i18n.LocalizedText;

/**
 * An APMC wholesale market (mandi): its live board, today's arrivals, helpline and AgriScan's price outlook. Admins
 * open and close trading, record arrivals and maintain the commodity prices ({@link CommodityPrice}); farmers see the
 * board in their language.
 */
@Entity
public class MandiMarket {

    /** The most arrivals a market can record for one day, in quintals. */
    public static final int MAX_ARRIVALS_QUINTALS = 1_000_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** "Solapur APMC Mandi" */
    @Embedded
    @EmbeddedColumnNaming("name_%s")
    private LocalizedText name;

    /** "Solapur Main APMC", shown on the market selector */
    @Embedded
    @EmbeddedColumnNaming("short_name_%s")
    private LocalizedText shortName;

    /** "Solapur" */
    @Embedded
    @EmbeddedColumnNaming("district_%s")
    private LocalizedText district;

    private boolean open;

    /** "Live Wholesale Board • आजचे बाजारभाव" */
    @Embedded
    @EmbeddedColumnNaming("board_label_%s")
    private LocalizedText boardLabel;

    private int arrivalsQuintals;
    private String imageUrl;

    @Embedded
    private MandiHelpline helpline;

    @Embedded
    private PriceForecast forecast;

    private Instant pricesUpdatedAt;

    protected MandiMarket() {
    }

    /** A market that is open for trading. */
    public MandiMarket(LocalizedText name, LocalizedText shortName, LocalizedText district, LocalizedText boardLabel,
                       String imageUrl, MandiHelpline helpline, PriceForecast forecast, int arrivalsQuintals,
                       Instant pricesUpdatedAt) {
        this.name = name;
        this.shortName = shortName;
        this.district = district;
        this.open = true;
        this.boardLabel = boardLabel;
        this.imageUrl = imageUrl;
        this.helpline = helpline;
        this.forecast = forecast;
        this.arrivalsQuintals = arrivalsQuintals;
        this.pricesUpdatedAt = pricesUpdatedAt;
    }

    /** Opens or closes trading; the board's status label follows. */
    public void changeOpen(boolean open) {
        this.open = open;
    }

    /** Records today's arrivals at the market yard. */
    public void recordArrivals(int quintals) {
        this.arrivalsQuintals = quintals;
    }

    /** Notes that the board's prices were revised at {@code at} ("Updated 18 min ago"). */
    public void markPricesUpdated(Instant at) {
        this.pricesUpdatedAt = at;
    }

    public Long getId() {
        return id;
    }

    public LocalizedText getName() {
        return name;
    }

    public LocalizedText getShortName() {
        return shortName;
    }

    public LocalizedText getDistrict() {
        return district;
    }

    public boolean isOpen() {
        return open;
    }

    public LocalizedText getBoardLabel() {
        return boardLabel;
    }

    public int getArrivalsQuintals() {
        return arrivalsQuintals;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public MandiHelpline getHelpline() {
        return helpline;
    }

    public PriceForecast getForecast() {
        return forecast;
    }

    public Instant getPricesUpdatedAt() {
        return pricesUpdatedAt;
    }
}
