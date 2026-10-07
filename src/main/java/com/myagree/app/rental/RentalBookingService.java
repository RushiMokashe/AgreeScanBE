package com.myagree.app.rental;

import java.time.Clock;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.account.AccountService;
import com.myagree.app.common.ConflictException;
import com.myagree.app.common.NotFoundException;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.farmer.FarmerService;
import com.myagree.app.farmer.dto.FarmerProfileResponse;
import com.myagree.app.rental.dto.BookRentalRequest;
import com.myagree.app.rental.dto.OwnerBookingDetailResponse;
import com.myagree.app.rental.dto.OwnerBookingResponse;
import com.myagree.app.rental.dto.RentalBookingDetailResponse;
import com.myagree.app.rental.dto.RentalBookingResponse;

/**
 * Bookings and their lifecycle (docs/architecture/phase-2.md, D9) from both sides: the farmer requests, follows and
 * may cancel; the vehicle's owner accepts or declines, starts and completes. Each side only reaches its own bookings;
 * anyone else's id answers 404. Every move is told to the other side.
 */
@Service
@Transactional(readOnly = true)
public class RentalBookingService {

    private static final String BOOKING_NOT_FOUND = "rental.booking.not-found";

    private final RentalBookingRepository bookingRepository;
    private final RentalListingRepository listingRepository;
    private final FarmerService farmerService;
    private final AccountService accountService;
    private final BookingMapper mapper;
    private final BookingNotifications notifications;
    private final Clock clock;

    RentalBookingService(RentalBookingRepository bookingRepository, RentalListingRepository listingRepository,
                         FarmerService farmerService, AccountService accountService, BookingMapper mapper,
                         BookingNotifications notifications, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.farmerService = farmerService;
        this.accountService = accountService;
        this.mapper = mapper;
        this.notifications = notifications;
        this.clock = clock;
    }

    /**
     * Asks the vehicle's owner for a booking and tells them about it. Without a slot the vehicle's next available one
     * is taken, in the farmer's language; without an estimate, the rate unit's default.
     *
     * @throws NotFoundException when there is no such listing
     * @throws ConflictException when the owner has taken the vehicle offline
     */
    @Transactional
    public RentalBookingResponse book(CurrentUser user, long listingId, BookRentalRequest request, Language language) {
        long farmerId = user.requireFarmerId();
        RentalListing listing = listingRepository.findWithDetailsByIdAndRemovedFalse(listingId)
                .orElseThrow(() -> RentalCatalogService.listingNotFound(listingId));
        FarmerProfileResponse profile = farmerService.currentFarmer();
        BookingFarmer farmer = new BookingFarmer(farmerId, user.userId(), profile.name(),
                accountService.get(user.userId()).phone(), profile.location());
        String slot = request.slotLabel() == null || request.slotLabel().isBlank()
                ? listing.getAvailability().resolve(language)
                : request.slotLabel().strip();
        int units = request.estimatedUnits() != null ? request.estimatedUnits() : listing.getRateUnit().defaultUnits();
        RentalBooking booking = bookingRepository.save(listing.book(farmer, slot, units, clock.instant()));
        notifications.requested(booking);
        return mapper.toFarmerResponse(booking, language);
    }

    /** The farmer's bookings, newest first. */
    public List<RentalBookingResponse> farmerBookings(long farmerId, Language language) {
        return bookingRepository.findForFarmer(farmerId).stream()
                .map(booking -> mapper.toFarmerResponse(booking, language))
                .toList();
    }

    /**
     * One of the farmer's bookings with its timeline.
     *
     * @throws NotFoundException when the farmer has no such booking
     */
    public RentalBookingDetailResponse farmerBooking(long farmerId, long bookingId, Language language) {
        return mapper.toFarmerDetail(findForFarmer(farmerId, bookingId), language);
    }

    /**
     * Calls the booking off while it is requested or accepted, and tells the owner.
     *
     * @throws NotFoundException when the farmer has no such booking
     * @throws ConflictException when the booking can no longer be cancelled
     */
    @Transactional
    public RentalBookingDetailResponse cancel(long farmerId, long bookingId, @Nullable String reason, Language language) {
        RentalBooking booking = findForFarmer(farmerId, bookingId);
        move(booking, BookingTransition.CANCEL, reason);
        return mapper.toFarmerDetail(booking, language);
    }

    /** The owner's bookings, optionally only those in {@code status}, newest first. */
    public List<OwnerBookingResponse> ownerBookings(long ownerId, @Nullable BookingStatus status, Language language) {
        return bookingRepository.findForOwner(ownerId, status).stream()
                .map(booking -> mapper.toOwnerResponse(booking, language))
                .toList();
    }

    /**
     * One of the owner's bookings with its timeline and the moves the owner can make.
     *
     * @throws NotFoundException when the owner has no such booking
     */
    public OwnerBookingDetailResponse ownerBooking(long ownerId, long bookingId, Language language) {
        return mapper.toOwnerDetail(findForOwner(ownerId, bookingId), language);
    }

    /**
     * Confirms a requested booking.
     *
     * @throws NotFoundException when the owner has no such booking
     * @throws ConflictException when the booking is no longer requested
     */
    @Transactional
    public OwnerBookingDetailResponse accept(long ownerId, long bookingId, Language language) {
        return moveForOwner(ownerId, bookingId, BookingTransition.ACCEPT, null, language);
    }

    /**
     * Turns a requested booking down.
     *
     * @throws NotFoundException when the owner has no such booking
     * @throws ConflictException when the booking is no longer requested
     */
    @Transactional
    public OwnerBookingDetailResponse decline(long ownerId, long bookingId, @Nullable String reason, Language language) {
        return moveForOwner(ownerId, bookingId, BookingTransition.DECLINE, reason, language);
    }

    /**
     * Starts an accepted job: the owner is on the way.
     *
     * @throws NotFoundException when the owner has no such booking
     * @throws ConflictException when the booking is not accepted
     */
    @Transactional
    public OwnerBookingDetailResponse start(long ownerId, long bookingId, Language language) {
        return moveForOwner(ownerId, bookingId, BookingTransition.START, null, language);
    }

    /**
     * Completes a job in progress.
     *
     * @throws NotFoundException when the owner has no such booking
     * @throws ConflictException when the job is not in progress
     */
    @Transactional
    public OwnerBookingDetailResponse complete(long ownerId, long bookingId, Language language) {
        return moveForOwner(ownerId, bookingId, BookingTransition.COMPLETE, null, language);
    }

    private OwnerBookingDetailResponse moveForOwner(long ownerId, long bookingId, BookingTransition transition,
                                                    @Nullable String reason, Language language) {
        RentalBooking booking = findForOwner(ownerId, bookingId);
        move(booking, transition, reason);
        return mapper.toOwnerDetail(booking, language);
    }

    private void move(RentalBooking booking, BookingTransition transition, @Nullable String reason) {
        booking.move(transition, reason, clock.instant());
        notifications.moved(booking, transition, reason);
    }

    private RentalBooking findForFarmer(long farmerId, long bookingId) {
        return bookingRepository.findOneForFarmer(bookingId, farmerId).orElseThrow(() -> bookingNotFound(bookingId));
    }

    private RentalBooking findForOwner(long ownerId, long bookingId) {
        return bookingRepository.findByIdAndOwnerId(bookingId, ownerId).orElseThrow(() -> bookingNotFound(bookingId));
    }

    private static NotFoundException bookingNotFound(long bookingId) {
        return new NotFoundException(UserMessage.of(BOOKING_NOT_FOUND, String.valueOf(bookingId)));
    }
}
