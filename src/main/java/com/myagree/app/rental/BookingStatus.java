package com.myagree.app.rental;

import java.util.Set;

/**
 * Where a booking stands in its lifecycle (docs/architecture/phase-2.md, D9); mirrors {@code BookingStatus} in
 * frontend/src/lib/types.ts. The moves between them are the {@link BookingTransition}s.
 */
public enum BookingStatus {

    /** The farmer asked; the owner has not answered yet. */
    REQUESTED,
    /** The owner confirmed the job. */
    ACCEPTED,
    /** The owner turned the request down. */
    DECLINED,
    /** The owner started the job and is on the way. */
    IN_PROGRESS,
    /** The owner finished the job. */
    COMPLETED,
    /** The farmer called the booking off. */
    CANCELLED;

    /** Waiting for the owner, or confirmed and not started yet: a vehicle with such bookings cannot be removed. */
    static final Set<BookingStatus> OPEN = Set.of(REQUESTED, ACCEPTED);

    /** Not finished yet: the bookings the farmer is still waiting on. */
    static final Set<BookingStatus> UNFINISHED = Set.of(REQUESTED, ACCEPTED, IN_PROGRESS);

    /** The owner has taken the job on, so the farmer may pay for it online. */
    static final Set<BookingStatus> PAYABLE = Set.of(ACCEPTED, IN_PROGRESS, COMPLETED);
}
