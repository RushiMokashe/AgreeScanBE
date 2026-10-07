package com.myagree.app.rental;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.myagree.app.account.AccountService;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.spi.NotificationType;
import com.myagree.app.common.spi.Notifier;

/**
 * Tells the other side of a booking what happened (docs/architecture/phase-2.md, D10): the owner about a new request
 * or a cancel and about an online payment, the farmer about the owner's moves. Called inside the transaction that made
 * the change, as the {@link Notifier} contract asks; vehicle names are given in the recipient's preferred language.
 * Without the notification feature, bookings work and nobody is told.
 */
@Component
class BookingNotifications {

    private static final String FARMER_ROUTE = "/bookings/";
    private static final String OWNER_ROUTE = "/owner/bookings/";
    private static final String NO_REASON = "";

    private final ObjectProvider<Notifier> notifiers;
    private final AccountService accountService;
    private final BookingMapper bookingMapper;

    BookingNotifications(ObjectProvider<Notifier> notifiers, AccountService accountService, BookingMapper bookingMapper) {
        this.notifiers = notifiers;
        this.accountService = accountService;
        this.bookingMapper = bookingMapper;
    }

    /** Tells the owner that a farmer asked to book one of their vehicles. */
    void requested(RentalBooking booking) {
        long ownerUserId = booking.getListing().getOwner().getUserId();
        send(ownerUserId, NotificationType.BOOKING_REQUESTED, details(booking, ownerUserId, null), OWNER_ROUTE + booking.getId());
    }

    /** Tells the farmer about the owner's move, or the owner about the farmer's cancel. */
    void moved(RentalBooking booking, BookingTransition transition, @Nullable String reason) {
        boolean byFarmer = transition.actor() == BookingActor.FARMER;
        long recipient = byFarmer ? booking.getListing().getOwner().getUserId() : booking.getFarmer().userId();
        String route = (byFarmer ? OWNER_ROUTE : FARMER_ROUTE) + booking.getId();
        send(recipient, transition.notification(), details(booking, recipient, reason), route);
    }

    /** Tells the owner that the farmer paid the booking online. */
    void paid(RentalBooking booking) {
        long ownerUserId = booking.getListing().getOwner().getUserId();
        Language language = preferredLanguage(ownerUserId);
        send(ownerUserId, NotificationType.PAYMENT_SUCCEEDED, Map.of(
                        Notifier.AMOUNT, String.valueOf(booking.getAmount()),
                        Notifier.DESCRIPTION, bookingMapper.paymentDescription(booking, language)),
                OWNER_ROUTE + booking.getId());
    }

    /** Every booking parameter a notification may need; the notifier keeps those its type uses. */
    private Map<String, String> details(RentalBooking booking, long recipientUserId, @Nullable String reason) {
        RentalListing listing = booking.getListing();
        return Map.of(
                Notifier.FARMER_NAME, booking.getFarmer().name(),
                Notifier.OWNER_NAME, listing.getOwner().getName(),
                Notifier.LISTING_NAME, listing.getName().resolve(preferredLanguage(recipientUserId)),
                Notifier.SLOT, booking.getSlotLabel(),
                Notifier.AMOUNT, String.valueOf(booking.getAmount()),
                Notifier.REASON, reason == null ? NO_REASON : reason.strip());
    }

    private Language preferredLanguage(long userId) {
        return accountService.get(userId).preferredLanguage();
    }

    private void send(long userId, NotificationType type, Map<String, String> params, String route) {
        notifiers.ifAvailable(notifier -> notifier.notify(userId, type, params, route));
    }
}
