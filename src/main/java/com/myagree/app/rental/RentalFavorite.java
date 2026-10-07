package com.myagree.app.rental;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** A listing one farmer saved; each farmer has their own favourites (docs/architecture/phase-2.md, D3). */
@Entity
@Table(name = "rental_favorite",
        uniqueConstraints = @UniqueConstraint(name = "rental_favorite_once", columnNames = {"farmer_id", "listing_id"}))
class RentalFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farmer_id", nullable = false, updatable = false)
    private long farmerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", updatable = false)
    private RentalListing listing;

    protected RentalFavorite() {
    }

    RentalFavorite(long farmerId, RentalListing listing) {
        this.farmerId = farmerId;
        this.listing = listing;
    }
}
