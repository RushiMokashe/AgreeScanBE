package com.myagree.app.rental.dto;

import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * Body of {@code POST /api/rentals/{id}/bookings}; may be empty.
 *
 * @param slotLabel requested slot, e.g. "Tomorrow 6:00 AM"; the listing's next available slot when omitted
 */
public record BookRentalRequest(@Size(max = BookRentalRequest.MAX_SLOT_LABEL_LENGTH) @Nullable String slotLabel) {

    static final int MAX_SLOT_LABEL_LENGTH = 80;
}
