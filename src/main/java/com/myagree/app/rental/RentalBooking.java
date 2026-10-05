package com.myagree.app.rental;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** A farmer's booking of a rental listing; created through {@link RentalListing#book}. */
@Entity
public class RentalBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id")
    private RentalListing listing;

    private String slotLabel;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private Instant createdAt;

    protected RentalBooking() {
    }

    RentalBooking(RentalListing listing, String slotLabel, Instant createdAt) {
        this.listing = listing;
        this.slotLabel = slotLabel;
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public RentalListing getListing() {
        return listing;
    }

    public String getSlotLabel() {
        return slotLabel;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
