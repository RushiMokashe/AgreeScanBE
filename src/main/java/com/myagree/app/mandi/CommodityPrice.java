package com.myagree.app.mandi;

import java.time.Instant;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Today's wholesale price band for one commodity at a market, in whole rupees per {@link Commodity#unit()}, with the
 * change of the maximum price since the previous revision. The minimum price never exceeds the maximum price; the
 * service that saves a price checks that first.
 */
@Entity
public class CommodityPrice {

    /** The lowest price a board accepts, in whole rupees. */
    public static final int MIN_PRICE = 1;
    /** The highest price a board accepts, in whole rupees. */
    public static final int MAX_PRICE = 1_000_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_id")
    private MandiMarket market;

    @Embedded
    private Commodity commodity;

    private int minPrice;
    private int maxPrice;

    /** New maximum price minus the previous one: +180 rose, 0 unchanged. */
    private int changeAmount;

    @Enumerated(EnumType.STRING)
    private DemandLevel demand;

    private Instant updatedAt;

    protected CommodityPrice() {
    }

    /**
     * @param changeAmount how far the maximum price moved at the last revision; 0 for a price new to the board
     */
    public CommodityPrice(MandiMarket market, Commodity commodity, int minPrice, int maxPrice, DemandLevel demand,
                          int changeAmount, Instant updatedAt) {
        this.market = market;
        this.commodity = commodity;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.demand = demand;
        this.changeAmount = changeAmount;
        this.updatedAt = updatedAt;
    }

    /** Replaces the quote; the change is measured from the maximum price it replaces. */
    public void revise(Commodity commodity, int minPrice, int maxPrice, DemandLevel demand, Instant at) {
        this.changeAmount = maxPrice - this.maxPrice;
        this.commodity = commodity;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.demand = demand;
        this.updatedAt = at;
    }

    public Long getId() {
        return id;
    }

    public MandiMarket getMarket() {
        return market;
    }

    public Commodity getCommodity() {
        return commodity;
    }

    public int getMinPrice() {
        return minPrice;
    }

    public int getMaxPrice() {
        return maxPrice;
    }

    public int getChangeAmount() {
        return changeAmount;
    }

    public DemandLevel getDemand() {
        return demand;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
