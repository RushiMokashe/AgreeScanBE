package com.myagree.app.common.spi;

import static com.myagree.app.common.spi.Notifier.AMOUNT;
import static com.myagree.app.common.spi.Notifier.DESCRIPTION;
import static com.myagree.app.common.spi.Notifier.FARMER_NAME;
import static com.myagree.app.common.spi.Notifier.LISTING_NAME;
import static com.myagree.app.common.spi.Notifier.OWNER_NAME;
import static com.myagree.app.common.spi.Notifier.PAYEE_NAME;
import static com.myagree.app.common.spi.Notifier.REASON;
import static com.myagree.app.common.spi.Notifier.SLOT;
import static com.myagree.app.common.spi.Notifier.UPI_REFERENCE;

import java.util.List;

/**
 * What a notification is about, and who receives it; mirrors {@code NotificationType} in frontend/src/lib/types.ts.
 * The notification slice renders each type from the message codes {@code notification.<TYPE>.title} and
 * {@code notification.<TYPE>.body}, whose placeholders {@code {0}}, {@code {1}}, ... are the type's
 * {@link #parameters()} in order.
 */
public enum NotificationType {

    /** To the owner: a farmer asked to book one of their vehicles. */
    BOOKING_REQUESTED(FARMER_NAME, LISTING_NAME, SLOT, AMOUNT),

    /** To the farmer: the owner accepted the booking. */
    BOOKING_ACCEPTED(OWNER_NAME, LISTING_NAME, SLOT),

    /** To the farmer: the owner declined the booking. */
    BOOKING_DECLINED(OWNER_NAME, LISTING_NAME, REASON),

    /** To the farmer: the owner started the job and is on the way. */
    BOOKING_STARTED(OWNER_NAME, LISTING_NAME),

    /** To the farmer: the owner completed the job. */
    BOOKING_COMPLETED(OWNER_NAME, LISTING_NAME, AMOUNT),

    /** To the owner: the farmer cancelled the booking. */
    BOOKING_CANCELLED(FARMER_NAME, LISTING_NAME, SLOT, REASON),

    /** To the payer, and for a booking also to the owner: a payment went through. */
    PAYMENT_SUCCEEDED(AMOUNT, DESCRIPTION),

    /** To the payer: a payment failed, nothing was charged, and they can pay again. */
    PAYMENT_FAILED(AMOUNT, DESCRIPTION, REASON),

    /** To the shopkeeper or vehicle owner: a farmer paid them by Scan & Pay and asks them to confirm it arrived. */
    PAYMENT_TO_CONFIRM(FARMER_NAME, AMOUNT, DESCRIPTION, UPI_REFERENCE),

    /** To the payer: the seller says a Scan & Pay payment did not reach them; the farmer can pay again. */
    PAYMENT_NOT_RECEIVED(PAYEE_NAME, AMOUNT, DESCRIPTION);

    private final List<String> parameters;

    NotificationType(String... parameters) {
        this.parameters = List.of(parameters);
    }

    /** The {@link Notifier} parameter keys this type's texts need, in the order of their placeholders. */
    public List<String> parameters() {
        return parameters;
    }
}
