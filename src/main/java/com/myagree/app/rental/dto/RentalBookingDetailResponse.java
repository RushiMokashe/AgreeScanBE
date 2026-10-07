package com.myagree.app.rental.dto;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.myagree.app.rental.BookingPaymentStatus;
import com.myagree.app.rental.BookingStatus;
import com.myagree.app.rental.RateUnit;

/**
 * Mirrors {@code RentalBookingDetail} in frontend/src/lib/types.ts: the farmer's tracking screen, which extends
 * {@link RentalBookingResponse} with the vehicle's picture, the timeline and what the farmer can do next.
 *
 * @param paymentId the payment that paid the booking online, once it is paid
 */
public record RentalBookingDetailResponse(
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
        String message,
        @Nullable String listingImageUrl,
        @Nullable String listingIcon,
        String hubName,
        List<BookingEventResponse> timeline,
        boolean canCancel,
        boolean canPay,
        @Nullable Long paymentId) {
}
