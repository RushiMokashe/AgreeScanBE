package com.myagree.app.rental;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;

/**
 * The featured transport offer at the top of the rentals screen. It is backed by its own listing (which is
 * what gets booked) and is kept out of the regular listing grid.
 */
@Entity
public class RentalSpotlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", unique = true)
    private RentalListing listing;

    private String label;
    private String imageUrl;
    private int baseFare;
    private int dispatchMinutes;

    @ElementCollection
    @CollectionTable(name = "rental_spotlight_perk", joinColumns = @JoinColumn(name = "spotlight_id"))
    @OrderColumn(name = "position")
    private List<RentalPerk> perks = new ArrayList<>();

    protected RentalSpotlight() {
    }

    public RentalSpotlight(RentalListing listing, String label, String imageUrl, int baseFare, int dispatchMinutes,
                           List<RentalPerk> perks) {
        this.listing = listing;
        this.label = label;
        this.imageUrl = imageUrl;
        this.baseFare = baseFare;
        this.dispatchMinutes = dispatchMinutes;
        this.perks = new ArrayList<>(perks);
    }

    public Long getId() {
        return id;
    }

    public RentalListing getListing() {
        return listing;
    }

    public String getLabel() {
        return label;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getBaseFare() {
        return baseFare;
    }

    public int getDispatchMinutes() {
        return dispatchMinutes;
    }

    public List<RentalPerk> getPerks() {
        return List.copyOf(perks);
    }
}
