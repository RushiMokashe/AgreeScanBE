package com.myagree.app.rental.dto;

import java.time.Instant;

import com.myagree.app.rental.BookingStatus;

/** Mirrors {@code RentalBooking} in frontend/src/lib/types.ts. */
public record RentalBookingResponse(
        long id,
        long listingId,
        String listingName,
        BookingStatus status,
        String slotLabel,
        Instant createdAt,
        String message) {
}
