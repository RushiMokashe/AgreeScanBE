package com.myagree.app.rental.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * Body of {@code POST /api/rentals/{id}/bookings}; mirrors {@code BookRentalRequest} in frontend/src/lib/types.ts and
 * may be empty.
 *
 * @param slotLabel      requested slot, e.g. "Tomorrow 6:00 AM"; the listing's next available slot when omitted
 * @param estimatedUnits hours, km or days per the listing's rate unit; its default estimate when omitted
 */
public record BookRentalRequest(
        @Size(max = BookRentalRequest.MAX_SLOT_LABEL_LENGTH) @Nullable String slotLabel,
        @Min(1) @Max(BookRentalRequest.MAX_UNITS) @Nullable Integer estimatedUnits) {

    public static final int MAX_SLOT_LABEL_LENGTH = 80;
    public static final int MAX_UNITS = 500;
}
