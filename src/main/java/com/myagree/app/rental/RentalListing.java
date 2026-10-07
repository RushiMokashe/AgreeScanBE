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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.EmbeddedColumnNaming;
import org.jspecify.annotations.Nullable;

import com.myagree.app.common.ConflictException;
import com.myagree.app.common.i18n.LocalizedText;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.media.StoredPhoto;

/**
 * A tractor, implement or mandi transport vehicle an owner offers for hire at a hub. Farmers see it while its owner
 * keeps it online. The owner edits what {@link ListingDetails} covers; AgriScan curates the rest of the card (badge,
 * distance, track record, button labels) for the vehicles it has verified. A removed listing disappears everywhere but
 * stays behind its past bookings.
 */
@Entity
@Table(name = "rental_listing", indexes = {
        @Index(name = "rental_listing_by_hub", columnList = "hub_id, online, removed"),
        @Index(name = "rental_listing_by_owner", columnList = "owner_id, removed")})
public class RentalListing {

    public static final int MAX_RATE = 100_000;
    public static final int MAX_SPECS = 3;
    public static final int MAX_FEATURES = 4;

    private static final UserMessage OFFLINE = UserMessage.of("rental.listing.offline");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private VehicleOwner owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hub_id")
    private RentalHub hub;

    @Enumerated(EnumType.STRING)
    private RentalCategory category;

    @Embedded
    @EmbeddedColumnNaming("name_%s")
    private LocalizedText name;

    @Embedded
    @EmbeddedColumnNaming("description_%s")
    private LocalizedText description;

    /** A photo shipped with the app, e.g. "/images/rentals/john-deere-5050d.jpg"; an uploaded photo replaces it. */
    private @Nullable String imageUrl;

    /** The owner's uploaded photo, served through a signed media URL. */
    @Embedded
    private @Nullable StoredPhoto photo;

    /** The Material Symbol of a card without a photo; {@code null} for the category's default. */
    private @Nullable String icon;

    @Embedded
    @EmbeddedColumnNaming("badge_%s")
    private @Nullable LocalizedText badge;

    private @Nullable String badgeIcon;

    private @Nullable Double distanceKm;

    @Embedded
    @EmbeddedColumnNaming("locality_%s")
    private @Nullable LocalizedText locality;

    private int rate;

    @Enumerated(EnumType.STRING)
    private RateUnit rateUnit;

    /** Charged on top of the distance rate by fare-based transport, such as the spotlight's pickup. */
    private @Nullable Integer baseFare;

    @Embedded
    @EmbeddedColumnNaming("rate_note_%s")
    private @Nullable LocalizedText rateNote;

    private boolean rateNoteHighlighted;

    /** The owner's track record, e.g. "120+ acres tilled • 4.9 ★★★★★"; new owners have none yet. */
    @Embedded
    @EmbeddedColumnNaming("operator_stats_%s")
    private @Nullable LocalizedText operatorStats;

    @Embedded
    @EmbeddedColumnNaming("availability_%s")
    private LocalizedText availability;

    private boolean availabilityHighlighted;

    @ElementCollection
    @CollectionTable(name = "rental_listing_spec", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "position")
    private List<RentalSpec> specs = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "rental_listing_feature", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "position")
    private List<RentalFeature> features = new ArrayList<>();

    /** "Call Rameshwar"; {@code null} shows the default with the owner's first name. */
    @Embedded
    @EmbeddedColumnNaming("call_label_%s")
    private @Nullable LocalizedText callLabel;

    /** "Reserve 4 AM Run"; {@code null} shows the category's default. */
    @Embedded
    @EmbeddedColumnNaming("book_label_%s")
    private @Nullable LocalizedText bookLabel;

    private @Nullable String bookIcon;

    private boolean online;

    private boolean removed;

    @Version
    private long version;

    protected RentalListing() {
    }

    /** A vehicle an owner lists through the owner portal. */
    RentalListing(VehicleOwner owner, ListingDetails details) {
        this.owner = owner;
        update(details);
    }

    /** Starts a vehicle AgriScan lists with a curated card, as the demo data does. */
    public static Builder builder(VehicleOwner owner, ListingDetails details) {
        return new Builder(new RentalListing(owner, details));
    }

    /** Replaces everything the owner edits; the curated parts of the card stay. */
    void update(ListingDetails details) {
        this.category = details.category();
        this.hub = details.hub();
        this.name = details.name();
        this.description = details.description();
        this.icon = details.icon();
        this.rate = details.rate();
        this.rateUnit = details.rateUnit();
        this.rateNote = details.rateNote();
        this.availability = details.availability();
        this.online = details.online();
        this.specs = new ArrayList<>(details.specs());
        this.features = new ArrayList<>(details.features());
    }

    /**
     * Books the vehicle for a farmer; without a slot the next available one ({@link #getAvailability()}) is taken.
     *
     * @param units hours, km or days, per the {@link #getRateUnit() rate unit}
     * @throws ConflictException when the owner has taken the vehicle offline
     */
    RentalBooking book(BookingFarmer farmer, String slotLabel, int units, Instant at) {
        if (!isBookable()) {
            throw new ConflictException(OFFLINE);
        }
        return new RentalBooking(this, farmer, slotLabel, units, at);
    }

    /** What a job of {@code units} costs: the rate per unit, plus the base fare of fare-based transport. */
    long priceFor(int units) {
        return (long) rate * units + Objects.requireNonNullElse(baseFare, 0);
    }

    boolean isBookable() {
        return online && !removed;
    }

    void changeOnline(boolean online) {
        this.online = online;
    }

    /** An admin's correction of the rate. */
    void reprice(int rate, RateUnit rateUnit) {
        this.rate = rate;
        this.rateUnit = rateUnit;
    }

    /** Shows the owner's photo instead of the one shipped with the app. */
    void replacePhoto(StoredPhoto photo) {
        this.photo = photo;
        this.imageUrl = null;
    }

    /** Takes the vehicle off AgriScan for good; its past bookings keep it. */
    void remove() {
        this.removed = true;
        this.online = false;
    }

    public Long getId() {
        return id;
    }

    public VehicleOwner getOwner() {
        return owner;
    }

    public RentalHub getHub() {
        return hub;
    }

    public RentalCategory getCategory() {
        return category;
    }

    public LocalizedText getName() {
        return name;
    }

    public LocalizedText getDescription() {
        return description;
    }

    public @Nullable String getImageUrl() {
        return imageUrl;
    }

    public @Nullable StoredPhoto getPhoto() {
        return photo;
    }

    public @Nullable String getIcon() {
        return icon;
    }

    public @Nullable LocalizedText getBadge() {
        return badge;
    }

    public @Nullable String getBadgeIcon() {
        return badgeIcon;
    }

    public @Nullable Double getDistanceKm() {
        return distanceKm;
    }

    public @Nullable LocalizedText getLocality() {
        return locality;
    }

    public int getRate() {
        return rate;
    }

    public RateUnit getRateUnit() {
        return rateUnit;
    }

    public @Nullable Integer getBaseFare() {
        return baseFare;
    }

    public @Nullable LocalizedText getRateNote() {
        return rateNote;
    }

    public boolean isRateNoteHighlighted() {
        return rateNoteHighlighted;
    }

    public @Nullable LocalizedText getOperatorStats() {
        return operatorStats;
    }

    public LocalizedText getAvailability() {
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

    public @Nullable LocalizedText getCallLabel() {
        return callLabel;
    }

    public @Nullable LocalizedText getBookLabel() {
        return bookLabel;
    }

    public @Nullable String getBookIcon() {
        return bookIcon;
    }

    public boolean isOnline() {
        return online;
    }

    public boolean isRemoved() {
        return removed;
    }

    /** The curated parts of a card AgriScan lists; see {@link #builder}. */
    public static final class Builder {

        private final RentalListing listing;

        private Builder(RentalListing listing) {
            this.listing = listing;
        }

        /** A photo shipped with the app. */
        public Builder photo(String imageUrl) {
            listing.imageUrl = imageUrl;
            return this;
        }

        public Builder badge(LocalizedText badge, String badgeIcon) {
            listing.badge = badge;
            listing.badgeIcon = badgeIcon;
            return this;
        }

        public Builder location(double distanceKm, LocalizedText locality) {
            listing.distanceKm = distanceKm;
            listing.locality = locality;
            return this;
        }

        public Builder baseFare(int baseFare) {
            listing.baseFare = baseFare;
            return this;
        }

        public Builder highlightRateNote() {
            listing.rateNoteHighlighted = true;
            return this;
        }

        public Builder highlightAvailability() {
            listing.availabilityHighlighted = true;
            return this;
        }

        public Builder operatorStats(LocalizedText stats) {
            listing.operatorStats = stats;
            return this;
        }

        public Builder labels(LocalizedText callLabel, LocalizedText bookLabel, String bookIcon) {
            listing.callLabel = callLabel;
            listing.bookLabel = bookLabel;
            listing.bookIcon = bookIcon;
            return this;
        }

        public RentalListing build() {
            return listing;
        }
    }
}
