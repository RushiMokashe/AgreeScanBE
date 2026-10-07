package com.myagree.app.rental;

import java.util.List;

import org.springframework.stereotype.Component;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.Messages;
import com.myagree.app.rental.dto.BookingEventResponse;
import com.myagree.app.rental.dto.OwnerBookingDetailResponse;
import com.myagree.app.rental.dto.OwnerBookingResponse;
import com.myagree.app.rental.dto.RentalBookingDetailResponse;
import com.myagree.app.rental.dto.RentalBookingResponse;

/** Shows a booking to the farmer who asked for it and to the vehicle's owner, in the reader's language. */
@Component
class BookingMapper {

    private static final String MESSAGE_PREFIX = "rental.booking.message.";
    private static final String PAYMENT_DESCRIPTION = "rental.payment.description";

    private final Messages messages;
    private final ListingPictures pictures;

    BookingMapper(Messages messages, ListingPictures pictures) {
        this.messages = messages;
        this.pictures = pictures;
    }

    RentalBookingResponse toFarmerResponse(RentalBooking booking, Language language) {
        RentalListing listing = booking.getListing();
        VehicleOwner owner = listing.getOwner();
        return new RentalBookingResponse(
                booking.getId(),
                listing.getId(),
                listing.getName().resolve(language),
                owner.getName(),
                owner.getPhone(),
                booking.getStatus(),
                booking.getSlotLabel(),
                booking.getEstimatedUnits(),
                booking.getRateUnit(),
                booking.getAmount(),
                booking.getPaymentStatus(),
                booking.getCreatedAt(),
                message(booking, language));
    }

    RentalBookingDetailResponse toFarmerDetail(RentalBooking booking, Language language) {
        RentalListing listing = booking.getListing();
        VehicleOwner owner = listing.getOwner();
        return new RentalBookingDetailResponse(
                booking.getId(),
                listing.getId(),
                listing.getName().resolve(language),
                owner.getName(),
                owner.getPhone(),
                booking.getStatus(),
                booking.getSlotLabel(),
                booking.getEstimatedUnits(),
                booking.getRateUnit(),
                booking.getAmount(),
                booking.getPaymentStatus(),
                booking.getCreatedAt(),
                message(booking, language),
                pictures.imageUrl(listing),
                pictures.icon(listing),
                listing.getHub().getName().resolve(language),
                timeline(booking),
                booking.can(BookingTransition.CANCEL),
                booking.isPayable(),
                booking.getPaymentId());
    }

    OwnerBookingResponse toOwnerResponse(RentalBooking booking, Language language) {
        BookingFarmer farmer = booking.getFarmer();
        return new OwnerBookingResponse(
                booking.getId(),
                booking.getListing().getId(),
                booking.getListing().getName().resolve(language),
                farmer.name(),
                farmer.phone(),
                farmer.location(),
                booking.getSlotLabel(),
                booking.getEstimatedUnits(),
                booking.getRateUnit(),
                booking.getAmount(),
                booking.getStatus(),
                booking.getPaymentStatus(),
                booking.getCreatedAt());
    }

    OwnerBookingDetailResponse toOwnerDetail(RentalBooking booking, Language language) {
        BookingFarmer farmer = booking.getFarmer();
        return new OwnerBookingDetailResponse(
                booking.getId(),
                booking.getListing().getId(),
                booking.getListing().getName().resolve(language),
                farmer.name(),
                farmer.phone(),
                farmer.location(),
                booking.getSlotLabel(),
                booking.getEstimatedUnits(),
                booking.getRateUnit(),
                booking.getAmount(),
                booking.getStatus(),
                booking.getPaymentStatus(),
                booking.getCreatedAt(),
                timeline(booking),
                booking.can(BookingTransition.ACCEPT),
                booking.can(BookingTransition.DECLINE),
                booking.can(BookingTransition.START),
                booking.can(BookingTransition.COMPLETE));
    }

    /** What paying the booking online is for, e.g. "Rental booking #12 • Tata Ace Gold (छोटा हाथी)". */
    String paymentDescription(RentalBooking booking, Language language) {
        return messages.get(PAYMENT_DESCRIPTION, language, String.valueOf(booking.getId()),
                booking.getListing().getName().resolve(language));
    }

    /** What the booking's status means for the farmer, e.g. "Request sent to Rameshwar Patil. ...". */
    private String message(RentalBooking booking, Language language) {
        return messages.get(MESSAGE_PREFIX + booking.getStatus().name(), language,
                booking.getListing().getOwner().getName());
    }

    private static List<BookingEventResponse> timeline(RentalBooking booking) {
        return booking.getTimeline().stream()
                .map(event -> new BookingEventResponse(event.status(), event.at(), event.actor(), event.note()))
                .toList();
    }
}
