package com.myagree.app.rental;

import java.time.Clock;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.NotFoundException;
import com.myagree.app.rental.dto.RentalBookingResponse;
import com.myagree.app.rental.dto.RentalListingResponse;
import com.myagree.app.rental.dto.RentalsOverviewResponse;

@Service
@Transactional(readOnly = true)
public class RentalService {

    private final RentalHubRepository hubRepository;
    private final RentalSpotlightRepository spotlightRepository;
    private final RentalListingRepository listingRepository;
    private final RentalBookingRepository bookingRepository;
    private final Clock clock;

    public RentalService(RentalHubRepository hubRepository, RentalSpotlightRepository spotlightRepository,
                         RentalListingRepository listingRepository, RentalBookingRepository bookingRepository,
                         Clock clock) {
        this.hubRepository = hubRepository;
        this.spotlightRepository = spotlightRepository;
        this.listingRepository = listingRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    /** The farmer's rental hub: the spotlight transport offer plus all other machinery and transport listings. */
    public RentalsOverviewResponse overview() {
        RentalHub hub = hubRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new NotFoundException("No rental hub serves the farmer's area"));
        RentalSpotlight spotlight = spotlightRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new NotFoundException("No spotlight rental offer is configured"));
        List<RentalListing> listings = listingRepository.findByIdNotOrderByIdAsc(spotlight.getListing().getId());
        return RentalMapper.toOverview(hub, spotlight, listings);
    }

    /**
     * Books a listing (the spotlight's listing included) for the requested slot, or for its next available
     * slot when none is given.
     */
    @Transactional
    public RentalBookingResponse book(long listingId, @Nullable String requestedSlot) {
        RentalListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> NotFoundException.of("Rental listing", listingId));
        RentalBooking booking = bookingRepository.save(listing.book(requestedSlot, clock.instant()));
        return RentalMapper.toResponse(booking);
    }

    /** Adds the listing to, or removes it from, the farmer's saved rentals. */
    @Transactional
    public RentalListingResponse toggleFavorite(long listingId) {
        RentalListing listing = listingRepository.findWithDetailsById(listingId)
                .orElseThrow(() -> NotFoundException.of("Rental listing", listingId));
        listing.toggleFavorite();
        return RentalMapper.toResponse(listing);
    }
}
