package com.myagree.app.rental;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

import org.jspecify.annotations.Nullable;

/** A tractor, implement or mandi transport vehicle offered for hire. */
@Entity
public class RentalListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private RentalCategory category;

    private String name;
    private String description;
    private @Nullable String imageUrl;
    private @Nullable String icon;
    private @Nullable String badge;
    private @Nullable String badgeIcon;
    private @Nullable Double distanceKm;
    private @Nullable String locality;
    private int rate;

    @Enumerated(EnumType.STRING)
    private RateUnit rateUnit;

    private @Nullable String rateNote;
    private boolean rateNoteHighlighted;

    @Embedded
    private RentalOperator operator;

    private String availability;
    private boolean availabilityHighlighted;

    @ElementCollection
    @CollectionTable(name = "rental_listing_spec", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "position")
    private List<RentalSpec> specs = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "rental_listing_feature", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "position")
    private List<RentalFeature> features = new ArrayList<>();

    private String phone;
    private String callLabel;
    private String bookLabel;
    private String bookIcon;
    private boolean favorite;

    protected RentalListing() {
    }

    public static Builder builder(RentalCategory category, String name, String description) {
        return new Builder(category, name, description);
    }

    /** Books the vehicle; without a requested slot the next available one ({@link #getAvailability()}) is taken. */
    public RentalBooking book(@Nullable String requestedSlot, Instant bookedAt) {
        String slot = requestedSlot == null || requestedSlot.isBlank() ? availability : requestedSlot.strip();
        return new RentalBooking(this, slot, bookedAt);
    }

    public void toggleFavorite() {
        favorite = !favorite;
    }

    public Long getId() {
        return id;
    }

    public RentalCategory getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public @Nullable String getImageUrl() {
        return imageUrl;
    }

    public @Nullable String getIcon() {
        return icon;
    }

    public @Nullable String getBadge() {
        return badge;
    }

    public @Nullable String getBadgeIcon() {
        return badgeIcon;
    }

    public @Nullable Double getDistanceKm() {
        return distanceKm;
    }

    public @Nullable String getLocality() {
        return locality;
    }

    public int getRate() {
        return rate;
    }

    public RateUnit getRateUnit() {
        return rateUnit;
    }

    public @Nullable String getRateNote() {
        return rateNote;
    }

    public boolean isRateNoteHighlighted() {
        return rateNoteHighlighted;
    }

    public RentalOperator getOperator() {
        return operator;
    }

    public String getAvailability() {
        return availability;
    }

    public boolean isAvailabilityHighlighted() {
        return availabilityHighlighted;
    }

    public List<RentalSpec> getSpecs() {
        return List.copyOf(specs);
    }

    public List<RentalFeature> getFeatures() {
        return List.copyOf(features);
    }

    public String getPhone() {
        return phone;
    }

    public String getCallLabel() {
        return callLabel;
    }

    public String getBookLabel() {
        return bookLabel;
    }

    public String getBookIcon() {
        return bookIcon;
    }

    public boolean isFavorite() {
        return favorite;
    }

    /** Builds a listing card; a photo or an icon, the rate, operator, availability, contact and booking are required. */
    public static final class Builder {

        private final RentalListing listing = new RentalListing();

        private Builder(RentalCategory category, String name, String description) {
            listing.category = category;
            listing.name = name;
            listing.description = description;
        }

        /** Machinery cards show a photo. */
        public Builder photo(String imageUrl) {
            listing.imageUrl = imageUrl;
            return this;
        }

        /** Transport cards show an icon tile instead of a photo. */
        public Builder icon(String icon) {
            listing.icon = icon;
            return this;
        }

        public Builder badge(String badge, String badgeIcon) {
            listing.badge = badge;
            listing.badgeIcon = badgeIcon;
            return this;
        }

        public Builder location(double distanceKm, String locality) {
            listing.distanceKm = distanceKm;
            listing.locality = locality;
            return this;
        }

        public Builder rate(int rate, RateUnit unit) {
            listing.rate = rate;
            listing.rateUnit = unit;
            return this;
        }

        public Builder rateNote(String note, boolean highlighted) {
            listing.rateNote = note;
            listing.rateNoteHighlighted = highlighted;
            return this;
        }

        public Builder operator(RentalOperator operator) {
            listing.operator = operator;
            return this;
        }

        public Builder availability(String availability, boolean highlighted) {
            listing.availability = availability;
            listing.availabilityHighlighted = highlighted;
            return this;
        }

        public Builder specs(List<RentalSpec> specs) {
            listing.specs = new ArrayList<>(specs);
            return this;
        }

        public Builder features(List<RentalFeature> features) {
            listing.features = new ArrayList<>(features);
            return this;
        }

        public Builder contact(String phone, String callLabel) {
            listing.phone = phone;
            listing.callLabel = callLabel;
            return this;
        }

        public Builder booking(String bookLabel, String bookIcon) {
            listing.bookLabel = bookLabel;
            listing.bookIcon = bookIcon;
            return this;
        }

        public RentalListing build() {
            if (listing.imageUrl == null && listing.icon == null) {
                throw new IllegalStateException("A rental listing needs a photo or an icon");
            }
            Objects.requireNonNull(listing.rateUnit, "rate");
            Objects.requireNonNull(listing.operator, "operator");
            Objects.requireNonNull(listing.availability, "availability");
            Objects.requireNonNull(listing.phone, "contact");
            Objects.requireNonNull(listing.bookLabel, "booking");
            return listing;
        }
    }
}
