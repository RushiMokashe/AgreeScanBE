package com.myagree.app.rental;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RentalBookingRepository extends JpaRepository<RentalBooking, Long> {

    /** The farmer's bookings, newest first. */
    @EntityGraph(attributePaths = {"listing", "listing.owner"})
    @Query("select b from RentalBooking b where b.farmer.id = :farmerId order by b.createdAt desc, b.id desc")
    List<RentalBooking> findForFarmer(long farmerId);

    /** The farmer's bookings in these states, oldest first. */
    @EntityGraph(attributePaths = {"listing", "listing.owner"})
    @Query("""
            select b from RentalBooking b where b.farmer.id = :farmerId and b.status in :statuses
            order by b.createdAt, b.id""")
    List<RentalBooking> findForFarmer(long farmerId, Collection<BookingStatus> statuses);

    /** One of the farmer's bookings with its timeline; another farmer's id finds nothing. */
    @EntityGraph(attributePaths = {"listing", "listing.owner", "listing.hub", "timeline"})
    @Query("select b from RentalBooking b where b.id = :id and b.farmer.id = :farmerId")
    Optional<RentalBooking> findOneForFarmer(long id, long farmerId);

    /** The owner's bookings, optionally in one status, newest first. */
    @EntityGraph(attributePaths = "listing")
    @Query("""
            select b from RentalBooking b where b.ownerId = :ownerId and (:status is null or b.status = :status)
            order by b.createdAt desc, b.id desc""")
    List<RentalBooking> findForOwner(long ownerId, @Nullable BookingStatus status);

    /** One of the owner's bookings with its timeline; another owner's id finds nothing. */
    @EntityGraph(attributePaths = {"listing", "listing.owner", "timeline"})
    Optional<RentalBooking> findByIdAndOwnerId(long id, long ownerId);

    /** The owner's next jobs: bookings in {@code status}, oldest request first. */
    @EntityGraph(attributePaths = "listing")
    List<RentalBooking> findTop5ByOwnerIdAndStatusOrderByCreatedAtAscIdAsc(long ownerId, BookingStatus status);

    /** A booking with what its notifications and payment need. */
    @EntityGraph(attributePaths = {"listing", "listing.owner"})
    Optional<RentalBooking> findWithListingById(long id);

    long countByOwnerIdAndStatus(long ownerId, BookingStatus status);

    long countByStatus(BookingStatus status);

    boolean existsByListingIdAndStatusIn(long listingId, Collection<BookingStatus> statuses);

    long countByListingIdAndStatusIn(long listingId, Collection<BookingStatus> statuses);

    /** How many of the owner's bookings in these states each listing has; listings without any are left out. */
    @Query("""
            select new com.myagree.app.rental.ListingBookingCount(b.listing.id, count(b)) from RentalBooking b
            where b.ownerId = :ownerId and b.status in :statuses group by b.listing.id""")
    List<ListingBookingCount> countPerListing(long ownerId, Collection<BookingStatus> statuses);

    /** Rupees the owner was paid online for jobs completed since {@code from}. */
    @Query("""
            select coalesce(sum(b.amount), 0) from RentalBooking b
            where b.ownerId = :ownerId and b.paymentStatus = :paid and b.completedAt >= :from""")
    long sumAmountCompletedSince(long ownerId, BookingPaymentStatus paid, Instant from);
}
