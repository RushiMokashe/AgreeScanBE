package com.myagree.app.farmer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.jspecify.annotations.Nullable;

/** A registered farmer: the farmer profile of one user account, with the hub and market they chose. */
@Entity
public class Farmer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The account that signs in as this farmer. */
    @Column(nullable = false, unique = true)
    private long userId;

    private String name;
    private String location;
    private String season;
    private String avatarUrl;

    /** The rental hub chosen on the rentals screen; {@code null} for the default hub. */
    private @Nullable Long preferredRentalHubId;

    /** The mandi market chosen on the mandi screen; {@code null} for the default market. */
    private @Nullable Long preferredMandiMarketId;

    protected Farmer() {
    }

    public Farmer(long userId, String name, String location, String season, String avatarUrl) {
        this.userId = userId;
        this.name = name;
        this.location = location;
        this.season = season;
        this.avatarUrl = avatarUrl;
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getSeason() {
        return season;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public @Nullable Long getPreferredRentalHubId() {
        return preferredRentalHubId;
    }

    public @Nullable Long getPreferredMandiMarketId() {
        return preferredMandiMarketId;
    }

    void preferRentalHub(long hubId) {
        this.preferredRentalHubId = hubId;
    }

    void preferMandiMarket(long marketId) {
        this.preferredMandiMarketId = marketId;
    }
}
