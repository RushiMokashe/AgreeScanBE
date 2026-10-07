package com.myagree.app.rental;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
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

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.ConflictException;

/**
 * A farmer's booking of a rental listing and its lifecycle (docs/architecture/phase-2.md, D9): requested by the
 * farmer, accepted or declined by the owner, started and completed by the owner, cancelled by the farmer while it is
 * requested or accepted. Every move is appended to the timeline. The rate and estimate are fixed when booking, so a
 * later price change never alters it. Created through {@link RentalListing#book}.
 */
@Entity
@Table(name = "rental_booking", indexes = {
        @Index(name = "rental_booking_by_farmer", columnList = "farmer_id, created_at"),
        @Index(name = "rental_booking_by_owner", columnList = "owner_id, status")})
public class RentalBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id")
    private RentalListing listing;

    /** The listing's owner, kept for the owner's queries. */
    @Column(name = "owner_id", nullable = false, updatable = false)
    private long ownerId;

    @Embedded
    private BookingFarmer farmer;

    @Column(nullable = false, updatable = false)
    private String slotLabel;

    @Column(nullable = false, updatable = false)
    private int estimatedUnits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private RateUnit rateUnit;

    /** Rate × units, plus the base fare of fare-based transport, in whole rupees. */
    @Column(nullable = false, updatable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingPaymentStatus paymentStatus;

    /** The payment that paid the booking online. */
    private @Nullable Long paymentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When the owner completed the job: the month whose earnings it counts towards. */
    @Column(name = "completed_at")
    private @Nullable Instant completedAt;

    @ElementCollection
    @CollectionTable(name = "rental_booking_event", joinColumns = @JoinColumn(name = "booking_id"))
    @OrderColumn(name = "position")
    private List<BookingEvent> timeline = new ArrayList<>();

    @Version
    private long version;

    protected RentalBooking() {
    }

    RentalBooking(RentalListing listing, BookingFarmer farmer, String slotLabel, int estimatedUnits, Instant at) {
        this.listing = listing;
        this.ownerId = listing.getOwner().getId();
        this.farmer = farmer;
        this.slotLabel = slotLabel;
        this.estimatedUnits = estimatedUnits;
        this.rateUnit = listing.getRateUnit();
        this.amount = listing.priceFor(estimatedUnits);
        this.status = BookingStatus.REQUESTED;
        this.paymentStatus = BookingPaymentStatus.UNPAID;
        this.createdAt = at;
        this.timeline.add(new BookingEvent(BookingStatus.REQUESTED, at, BookingActor.FARMER, null));
    }

    /**
     * Makes a move of the lifecycle and records it on the timeline.
     *
     * @param reason the decline or cancel reason; blank counts as none
     * @throws ConflictException when the booking's current status does not allow the move
     */
    void move(BookingTransition transition, @Nullable String reason, Instant at) {
        if (!can(transition)) {
            throw new ConflictException(transition.refusal());
        }
        status = transition.target();
        if (status == BookingStatus.COMPLETED) {
            completedAt = at;
        }
        String note = reason == null || reason.isBlank() ? null : reason.strip();
        timeline.add(new BookingEvent(status, at, transition.actor(), note));
    }

    boolean can(BookingTransition transition) {
        return transition.allowedFrom(status);
    }

    /** Whether the farmer can pay online now: the owner took the job on and it is not paid yet. */
    boolean isPayable() {
        return BookingStatus.PAYABLE.contains(status) && paymentStatus == BookingPaymentStatus.UNPAID;
    }

    /**
     * Records the online payment that paid the booking.
     *
     * @return {@code false}, changing nothing, when the booking was already paid
     */
    boolean markPaid(long paymentId) {
        if (paymentStatus == BookingPaymentStatus.PAID) {
            return false;
        }
        this.paymentStatus = BookingPaymentStatus.PAID;
        this.paymentId = paymentId;
        return true;
    }

    public Long getId() {
        return id;
    }

    public RentalListing getListing() {
        return listing;
    }

    public long getOwnerId() {
        return ownerId;
    }

    BookingFarmer getFarmer() {
        return farmer;
    }

    public String getSlotLabel() {
        return slotLabel;
    }

    public int getEstimatedUnits() {
        return estimatedUnits;
    }

    public RateUnit getRateUnit() {
        return rateUnit;
    }

    public long getAmount() {
        return amount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BookingPaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public @Nullable Long getPaymentId() {
        return paymentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Instant getCompletedAt() {
        return completedAt;
    }

    List<BookingEvent> getTimeline() {
        return List.copyOf(timeline);
    }
}
