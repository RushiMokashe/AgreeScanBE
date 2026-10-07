package com.myagree.app.rental.dto;

import java.time.Instant;

import com.myagree.app.rental.BookingPaymentStatus;
import com.myagree.app.rental.BookingStatus;
import com.myagree.app.rental.RateUnit;

/**
 * Mirrors {@code RentalBooking} in frontend/src/lib/types.ts: a booking as the farmer sees it.
 *
 * @param operatorPhone the owner's 10-digit mobile number
 * @param message       what the booking's status means for the farmer, in their language
 */
public record RentalBookingResponse(
        long id,
        long listingId,
        String listingName,
        String operatorName,
        String operatorPhone,
        BookingStatus status,
        String slotLabel,
        int estimatedUnits,
        RateUnit rateUnit,
        long amount,
        BookingPaymentStatus paymentStatus,
        Instant createdAt,
        String message) {
}
