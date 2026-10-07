package com.myagree.app.rental;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.i18n.IndianNumbers;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.Messages;
import com.myagree.app.common.spi.FarmContextContributor;
import com.myagree.app.common.spi.FarmContextSection;

/**
 * What the voice assistant knows about a farmer's rentals: their bookings still under way, and the vehicles taking
 * bookings at their hub.
 */
@Component
class RentalFarmContext implements FarmContextContributor {

    private static final String TITLE = "rental.context.title";
    private static final String BOOKING_LINE = "rental.context.booking";
    private static final String HUB_LINE = "rental.context.hub";
    private static final String STATUS_PREFIX = "rental.context.status.";
    private static final int EXAMPLE_VEHICLES = 3;
    private static final String LIST_SEPARATOR = ", ";
    private static final String LINE_SEPARATOR = "\n";

    private final RentalBookingRepository bookingRepository;
    private final RentalCatalogService catalogService;
    private final Messages messages;

    RentalFarmContext(RentalBookingRepository bookingRepository, RentalCatalogService catalogService, Messages messages) {
        this.bookingRepository = bookingRepository;
        this.catalogService = catalogService;
        this.messages = messages;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FarmContextSection> contribute(long farmerId, Language language) {
        List<String> lines = new ArrayList<>();
        for (RentalBooking booking : bookingRepository.findForFarmer(farmerId, BookingStatus.UNFINISHED)) {
            RentalListing listing = booking.getListing();
            lines.add(messages.get(BOOKING_LINE, language,
                    listing.getName().resolve(language),
                    listing.getOwner().getName(),
                    messages.get(STATUS_PREFIX + booking.getStatus().name(), language),
                    booking.getSlotLabel(),
                    IndianNumbers.rupees(booking.getAmount())));
        }
        catalogService.homeHub(farmerId).ifPresent(hub -> {
            List<RentalListing> listings = catalogService.bookableListings(hub);
            if (!listings.isEmpty()) {
                lines.add(messages.get(HUB_LINE, language,
                        hub.getName().resolve(language),
                        String.valueOf(listings.size()),
                        listings.stream().limit(EXAMPLE_VEHICLES)
                                .map(listing -> listing.getName().resolve(language))
                                .collect(Collectors.joining(LIST_SEPARATOR))));
            }
        });
        return lines.isEmpty()
                ? Optional.empty()
                : Optional.of(new FarmContextSection(messages.get(TITLE, language), String.join(LINE_SEPARATOR, lines)));
    }
}
