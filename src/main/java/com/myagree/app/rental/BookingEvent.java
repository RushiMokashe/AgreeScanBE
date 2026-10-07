package com.myagree.app.rental;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import org.jspecify.annotations.Nullable;

/**
 * One step of a booking's timeline (docs/architecture/phase-2.md, D9), shown to the farmer and the owner alike.
 *
 * @param note the decline or cancel reason as typed; {@code null} when none was given
 */
@Embeddable
record BookingEvent(
        @Enumerated(EnumType.STRING) @Column(name = "event_status", nullable = false) BookingStatus status,
        @Column(name = "event_at", nullable = false) Instant at,
        @Enumerated(EnumType.STRING) @Column(nullable = false) BookingActor actor,
        @Column(length = BookingEvent.NOTE_MAX_LENGTH) @Nullable String note) {

    static final int NOTE_MAX_LENGTH = 200;
}
