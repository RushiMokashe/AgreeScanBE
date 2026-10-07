package com.myagree.app.rental.dto;

import java.time.Instant;
import java.util.List;

import com.myagree.app.rental.BookingPaymentStatus;
import com.myagree.app.rental.BookingStatus;
import com.myagree.app.rental.RateUnit;

/**
 * Mirrors {@code OwnerBookingDetail} in frontend/src/lib/types.ts: {@link OwnerBookingResponse} with the timeline the
 * farmer sees too and the moves the owner can make now.
 */
public record OwnerBookingDetailResponse(
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
        Instant createdAt,
        List<BookingEventResponse> timeline,
        boolean canAccept,
        boolean canDecline,
        boolean canStart,
        boolean canComplete) {
}
