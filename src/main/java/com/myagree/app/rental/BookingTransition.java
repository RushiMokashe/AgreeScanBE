package com.myagree.app.rental;

import static com.myagree.app.rental.BookingStatus.ACCEPTED;
import static com.myagree.app.rental.BookingStatus.CANCELLED;
import static com.myagree.app.rental.BookingStatus.COMPLETED;
import static com.myagree.app.rental.BookingStatus.DECLINED;
import static com.myagree.app.rental.BookingStatus.IN_PROGRESS;
import static com.myagree.app.rental.BookingStatus.REQUESTED;

import java.util.Set;

import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.spi.NotificationType;

/**
 * The moves of the booking lifecycle (docs/architecture/phase-2.md, D9) after the farmer's request: who makes each
 * move, the states it starts from, the state it leads to, why it is refused elsewhere, and whom it notifies.
 */
enum BookingTransition {

    ACCEPT(BookingActor.OWNER, ACCEPTED, NotificationType.BOOKING_ACCEPTED, "rental.booking.cannot-accept", REQUESTED),
    DECLINE(BookingActor.OWNER, DECLINED, NotificationType.BOOKING_DECLINED, "rental.booking.cannot-decline", REQUESTED),
    START(BookingActor.OWNER, IN_PROGRESS, NotificationType.BOOKING_STARTED, "rental.booking.cannot-start", ACCEPTED),
    COMPLETE(BookingActor.OWNER, COMPLETED, NotificationType.BOOKING_COMPLETED, "rental.booking.cannot-complete",
            IN_PROGRESS),
    CANCEL(BookingActor.FARMER, CANCELLED, NotificationType.BOOKING_CANCELLED, "rental.booking.cannot-cancel",
            REQUESTED, ACCEPTED);

    private final BookingActor actor;
    private final BookingStatus target;
    private final NotificationType notification;
    private final UserMessage refusal;
    private final Set<BookingStatus> sources;

    BookingTransition(BookingActor actor, BookingStatus target, NotificationType notification, String refusalCode,
                      BookingStatus... sources) {
        this.actor = actor;
        this.target = target;
        this.notification = notification;
        this.refusal = UserMessage.of(refusalCode);
        this.sources = Set.of(sources);
    }

    /** Whether a booking in {@code status} can make this move. */
    boolean allowedFrom(BookingStatus status) {
        return sources.contains(status);
    }

    BookingActor actor() {
        return actor;
    }

    BookingStatus target() {
        return target;
    }

    /** What the other side of the booking is told: the farmer about the owner's moves, the owner about a cancel. */
    NotificationType notification() {
        return notification;
    }

    /** Why the move is refused from any state it does not start from. */
    UserMessage refusal() {
        return refusal;
    }
}
