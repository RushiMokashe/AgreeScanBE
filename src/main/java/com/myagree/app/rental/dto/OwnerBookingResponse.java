package com.myagree.app.rental.dto;

import java.time.Instant;

import com.myagree.app.rental.BookingPaymentStatus;
import com.myagree.app.rental.BookingStatus;
import com.myagree.app.rental.RateUnit;

/**
 * Mirrors {@code OwnerBooking} in frontend/src/lib/types.ts: a booking as the vehicle's owner sees it.
 *
 * @param farmerPhone the farmer's 10-digit mobile number
 */
public record OwnerBookingResponse(
        long id,
        long listingId,
        String listingName,
        String farmerName,
        String farmerPhone,
        String farmerLocation,
        String slotLabel,
        int estimatedUnits,
        RateUnit rateUnit,
        long amount,
        BookingStatus status,
        BookingPaymentStatus paymentStatus,
        Instant createdAt) {
}
