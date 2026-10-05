package com.myagree.app.rental;

import java.util.List;

import com.myagree.app.rental.dto.RentalBookingResponse;
import com.myagree.app.rental.dto.RentalFeatureResponse;
import com.myagree.app.rental.dto.RentalListingResponse;
import com.myagree.app.rental.dto.RentalOperatorResponse;
import com.myagree.app.rental.dto.RentalPerkResponse;
import com.myagree.app.rental.dto.RentalSpecResponse;
import com.myagree.app.rental.dto.RentalSpotlightResponse;
import com.myagree.app.rental.dto.RentalsOverviewResponse;

final class RentalMapper {

    private static final String BOOKING_CONFIRMED_MESSAGE = "Booked! %s will call you shortly to confirm.";

    private RentalMapper() {
    }

    static RentalsOverviewResponse toOverview(RentalHub hub, RentalSpotlight spotlight, List<RentalListing> listings) {
        return new RentalsOverviewResponse(
                hub.getName(),
                hub.getRadiusKm(),
                hub.getRoutes(),
                hub.getOnlineCount(),
                toResponse(spotlight),
                listings.stream().map(RentalMapper::toResponse).toList());
    }

    static RentalListingResponse toResponse(RentalListing listing) {
        RentalOperator operator = listing.getOperator();
        return new RentalListingResponse(
                listing.getId(),
                listing.getCategory(),
                listing.getName(),
                listing.getDescription(),
                listing.getImageUrl(),
                listing.getIcon(),
                listing.getBadge(),
                listing.getBadgeIcon(),
                listing.getDistanceKm(),
                listing.getLocality(),
                listing.getRate(),
                listing.getRateUnit(),
                listing.getRateNote(),
                listing.isRateNoteHighlighted(),
                new RentalOperatorResponse(operator.name(), operator.initials(), operator.stats()),
                listing.getAvailability(),
                listing.isAvailabilityHighlighted(),
                listing.getSpecs().stream().map(spec -> new RentalSpecResponse(spec.value(), spec.label())).toList(),
                listing.getFeatures().stream().map(feature -> new RentalFeatureResponse(feature.icon(), feature.text())).toList(),
                listing.getPhone(),
                listing.getCallLabel(),
                listing.getBookLabel(),
                listing.getBookIcon(),
                listing.isFavorite());
    }

    static RentalBookingResponse toResponse(RentalBooking booking) {
        RentalListing listing = booking.getListing();
        return new RentalBookingResponse(
                booking.getId(),
                listing.getId(),
                listing.getName(),
                booking.getStatus(),
                booking.getSlotLabel(),
                booking.getCreatedAt(),
                BOOKING_CONFIRMED_MESSAGE.formatted(listing.getOperator().name()));
    }

    private static RentalSpotlightResponse toResponse(RentalSpotlight spotlight) {
        RentalListing listing = spotlight.getListing();
        return new RentalSpotlightResponse(
                listing.getId(),
                listing.getName(),
                listing.getDescription(),
                spotlight.getLabel(),
                spotlight.getImageUrl(),
                spotlight.getBaseFare(),
                listing.getRate(),
                spotlight.getDispatchMinutes(),
                spotlight.getPerks().stream()
                        .map(perk -> new RentalPerkResponse(perk.icon(), perk.title(), perk.subtitle()))
                        .toList());
    }
}
