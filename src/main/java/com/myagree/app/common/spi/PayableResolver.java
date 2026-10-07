package com.myagree.app.common.spi;

import java.util.Optional;

import com.myagree.app.common.ConflictException;
import com.myagree.app.common.i18n.Language;

/**
 * Describes the order or booking a farmer asks to pay online ({@code POST /api/payments}).
 *
 * <p><b>Implemented by</b> the store slice for {@link PaymentPurpose#STORE_ORDER} (orders placed with the
 * {@code ONLINE} payment method that are {@code AWAITING_PAYMENT}) and the rental slice for
 * {@link PaymentPurpose#RENTAL_BOOKING} (bookings that are {@code ACCEPTED}, {@code IN_PROGRESS} or {@code COMPLETED}
 * and still unpaid). <b>Consumed by</b> the payment slice, which injects {@code List<PayableResolver>} and asks the one
 * whose {@link #purpose()} the request names.
 *
 * <p><b>Errors:</b> an empty result means there is no such order or booking, or it belongs to another farmer; the
 * payment slice answers 404 either way, so other farmers' ids never leak. A {@link ConflictException} means it exists
 * but cannot be paid in its current state (already paid, cancelled, not yet accepted), answered with 409. Resolvers
 * only read, inside the payment slice's transaction.
 */
public interface PayableResolver {

    /** The purpose this resolver serves; exactly one resolver serves each purpose. */
    PaymentPurpose purpose();

    /**
     * @param referenceId the order or booking id from the request
     * @param farmerId    the signed-in farmer; references of other farmers resolve to empty
     * @param language    the request's language, for {@link Payable#description()}
     * @throws ConflictException when the order or booking cannot be paid in its current state
     */
    Optional<Payable> resolvePayable(long referenceId, long farmerId, Language language);

    /**
     * What a payment of the order or booking is for, in {@code language}, whatever state it is in now; the payment
     * slice shows it with payments it has already scoped to the signed-in farmer, in the reader's language rather
     * than the one the payment was opened in.
     *
     * @param referenceId the order or booking id of a payment
     * @param language    the request's language
     * @return the description, e.g. "Agro Store order #12", or empty when the order or booking no longer exists
     */
    Optional<String> describe(long referenceId, Language language);
}
