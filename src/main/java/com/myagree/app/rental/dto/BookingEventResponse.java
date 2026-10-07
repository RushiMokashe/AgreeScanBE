package com.myagree.app.rental.dto;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import com.myagree.app.rental.BookingActor;
import com.myagree.app.rental.BookingStatus;

/** Mirrors {@code BookingEvent} in frontend/src/lib/types.ts. */
public record BookingEventResponse(BookingStatus status, Instant at, BookingActor actor, @Nullable String note) {
}
