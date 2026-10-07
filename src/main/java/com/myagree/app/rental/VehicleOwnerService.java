package com.myagree.app.rental;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.myagree.app.account.AccountService;
import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.ClockConfig;
import com.myagree.app.common.ConflictException;
import com.myagree.app.common.NotFoundException;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.media.PhotoStore;
import com.myagree.app.rental.dto.OwnerDashboardResponse;
import com.myagree.app.rental.dto.OwnerListingRequest;
import com.myagree.app.rental.dto.OwnerListingResponse;
import com.myagree.app.rental.dto.OwnerProfileRequest;
import com.myagree.app.rental.dto.VehicleOwnerProfileResponse;

/**
 * The vehicle-owner portal (docs/architecture/phase-2.md, D8): the owner's profile, their dashboard and their
 * vehicles. Every method is scoped to the signed-in owner; another owner's vehicle answers 404.
 */
@Service
@Transactional(readOnly = true)
public class VehicleOwnerService {

    private static final String OWNER_NOT_FOUND = "rental.owner.not-found";
    private static final String HUB_UNKNOWN = "rental.hub.unknown";
    private static final UserMessage HAS_OPEN_BOOKINGS = UserMessage.of("rental.listing.has-open-bookings");

    private final VehicleOwnerRepository ownerRepository;
    private final RentalHubRepository hubRepository;
    private final RentalListingRepository listingRepository;
    private final RentalBookingRepository bookingRepository;
    private final AccountService accountService;
    private final PhotoStore photoStore;
    private final OwnerMapper ownerMapper;
    private final BookingMapper bookingMapper;
    private final Clock clock;

    VehicleOwnerService(VehicleOwnerRepository ownerRepository, RentalHubRepository hubRepository,
                        RentalListingRepository listingRepository, RentalBookingRepository bookingRepository,
                        AccountService accountService, PhotoStore photoStore, OwnerMapper ownerMapper,
                        BookingMapper bookingMapper, Clock clock) {
        this.ownerRepository = ownerRepository;
        this.hubRepository = hubRepository;
        this.listingRepository = listingRepository;
        this.bookingRepository = bookingRepository;
        this.accountService = accountService;
        this.photoStore = photoStore;
        this.ownerMapper = ownerMapper;
        this.bookingMapper = bookingMapper;
        this.clock = clock;
    }

    /**
     * Creates the vehicle-owner profile of a new account and links it to the account.
     *
     * @throws BadRequestException for an unknown hub
     */
    @Transactional
    long createProfile(long userId, String name, @Nullable String businessName, String phone, long hubId) {
        VehicleOwner owner = ownerRepository.save(new VehicleOwner(userId, name, businessName, phone, knownHub(hubId)));
        accountService.linkOwnerProfile(userId, owner.getId());
        return owner.getId();
    }

    /** The signed-in owner's profile. */
    public VehicleOwnerProfileResponse profile(long ownerId, Language language) {
        return ownerMapper.toResponse(findOwner(ownerId), language);
    }

    /**
     * Changes the name farmers see, the business name and the hub the owner serves.
     *
     * @throws BadRequestException for an unknown hub
     */
    @Transactional
    public VehicleOwnerProfileResponse updateProfile(long ownerId, OwnerProfileRequest request, Language language) {
        VehicleOwner owner = findOwner(ownerId);
        owner.update(request.name(), request.businessName(), knownHub(request.hubId()));
        owner.changeUpiId(request.upiId());
        return ownerMapper.toResponse(owner, language);
    }

    /** Shows {@code image} (a photo or screenshot of the owner's UPI QR) to farmers who pay by Scan & Pay. */
    @Transactional
    public VehicleOwnerProfileResponse uploadUpiQr(long ownerId, MultipartFile image, Language language) {
        VehicleOwner owner = findOwner(ownerId);
        owner.replaceUpiQr(photoStore.store(ListingPictures.OWNER_QR_KIND, image));
        return ownerMapper.toResponse(owner, language);
    }

    @Transactional
    public VehicleOwnerProfileResponse removeUpiQr(long ownerId, Language language) {
        VehicleOwner owner = findOwner(ownerId);
        owner.removeUpiQr();
        return ownerMapper.toResponse(owner, language);
    }

    /** The owner's vehicles and bookings at a glance, with this month's online earnings (India time). */
    public OwnerDashboardResponse dashboard(long ownerId, Language language) {
        Instant monthStart = LocalDate.ofInstant(clock.instant(), ClockConfig.FARM_ZONE).withDayOfMonth(1)
                .atStartOfDay(ClockConfig.FARM_ZONE).toInstant();
        return new OwnerDashboardResponse(
                listingRepository.countByOwnerIdAndRemovedFalse(ownerId),
                listingRepository.countByOwnerIdAndOnlineTrueAndRemovedFalse(ownerId),
                bookingRepository.countByOwnerIdAndStatus(ownerId, BookingStatus.REQUESTED),
                bookingRepository.sumAmountCompletedSince(ownerId, BookingPaymentStatus.PAID, monthStart),
                bookingRepository.findTop5ByOwnerIdAndStatusOrderByCreatedAtAscIdAsc(ownerId, BookingStatus.ACCEPTED)
                        .stream()
                        .map(booking -> bookingMapper.toOwnerResponse(booking, language))
                        .toList());
    }

    /** The owner's vehicles, oldest first, each with its open bookings counted. */
    public List<OwnerListingResponse> listings(long ownerId) {
        Map<Long, Long> openBookings = openBookingsPerListing(ownerId);
        return listingRepository.findByOwnerIdAndRemovedFalseOrderByIdAsc(ownerId).stream()
                .map(listing -> ownerMapper.toResponse(listing, openBookings.getOrDefault(listing.getId(), 0L)))
                .toList();
    }

    /**
     * One of the owner's vehicles.
     *
     * @throws NotFoundException when the owner has no such vehicle
     */
    public OwnerListingResponse listing(long ownerId, long listingId) {
        return toResponse(findListing(ownerId, listingId));
    }

    /**
     * Lists a new vehicle.
     *
     * @throws BadRequestException for an unknown hub
     */
    @Transactional
    public OwnerListingResponse create(long ownerId, OwnerListingRequest request) {
        RentalListing listing = new RentalListing(findOwner(ownerId), OwnerMapper.toDetails(request, knownHub(request.hubId())));
        return ownerMapper.toResponse(listingRepository.save(listing), 0);
    }

    /**
     * Replaces what the owner says about a vehicle; bookings already made keep their price.
     *
     * @throws NotFoundException   when the owner has no such vehicle
     * @throws BadRequestException for an unknown hub
     */
    @Transactional
    public OwnerListingResponse update(long ownerId, long listingId, OwnerListingRequest request) {
        RentalListing listing = findListing(ownerId, listingId);
        listing.update(OwnerMapper.toDetails(request, knownHub(request.hubId())));
        return toResponse(listing);
    }

    /**
     * Starts or stops taking bookings for a vehicle; bookings already made stay.
     *
     * @throws NotFoundException when the owner has no such vehicle
     */
    @Transactional
    public OwnerListingResponse setOnline(long ownerId, long listingId, boolean online) {
        RentalListing listing = findListing(ownerId, listingId);
        listing.changeOnline(online);
        return toResponse(listing);
    }

    /**
     * Shows the uploaded photo on the vehicle's card.
     *
     * @throws NotFoundException   when the owner has no such vehicle
     * @throws BadRequestException when the upload is empty or not a photo
     */
    @Transactional
    public OwnerListingResponse uploadPhoto(long ownerId, long listingId, MultipartFile image) {
        RentalListing listing = findListing(ownerId, listingId);
        listing.replacePhoto(photoStore.store(ListingPictures.MEDIA_KIND, image));
        return toResponse(listing);
    }

    /**
     * Takes a vehicle off AgriScan for good; its past bookings keep it.
     *
     * @throws NotFoundException when the owner has no such vehicle
     * @throws ConflictException while it has requested or accepted bookings
     */
    @Transactional
    public void delete(long ownerId, long listingId) {
        RentalListing listing = findListing(ownerId, listingId);
        if (bookingRepository.existsByListingIdAndStatusIn(listingId, BookingStatus.OPEN)) {
            throw new ConflictException(HAS_OPEN_BOOKINGS);
        }
        listing.remove();
    }

    private OwnerListingResponse toResponse(RentalListing listing) {
        return ownerMapper.toResponse(listing,
                bookingRepository.countByListingIdAndStatusIn(listing.getId(), BookingStatus.OPEN));
    }

    private Map<Long, Long> openBookingsPerListing(long ownerId) {
        return bookingRepository.countPerListing(ownerId, BookingStatus.OPEN).stream()
                .collect(Collectors.toMap(ListingBookingCount::listingId, ListingBookingCount::bookings));
    }

    private VehicleOwner findOwner(long ownerId) {
        return ownerRepository.findWithHubById(ownerId)
                .orElseThrow(() -> new NotFoundException(UserMessage.of(OWNER_NOT_FOUND, String.valueOf(ownerId))));
    }

    private RentalListing findListing(long ownerId, long listingId) {
        return listingRepository.findByIdAndOwnerIdAndRemovedFalse(listingId, ownerId)
                .orElseThrow(() -> RentalCatalogService.listingNotFound(listingId));
    }

    private RentalHub knownHub(long hubId) {
        return hubRepository.findById(hubId)
                .orElseThrow(() -> new BadRequestException(UserMessage.of(HUB_UNKNOWN, String.valueOf(hubId))));
    }
}
