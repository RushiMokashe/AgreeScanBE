package com.myagree.app.common.spi;

import java.time.Instant;

/**
 * A payment succeeded, so the order or booking it pays for is paid.
 *
 * <p><b>Published by</b> the payment slice, once per payment, when the payment becomes {@code SUCCEEDED} (through the
 * provider's webhook or the client's confirmation, whichever comes first): with {@code ApplicationEventPublisher},
 * inside the transaction that records it. <b>Consumed by</b> the store slice for {@link PaymentPurpose#STORE_ORDER}
 * (the order becomes {@code PAID}) and the rental slice for {@link PaymentPurpose#RENTAL_BOOKING} (the booking becomes
 * {@code PAID}); each ignores the other purpose. They listen with
 * <pre>{@code
 * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
 * @Transactional(propagation = Propagation.REQUIRES_NEW)
 * void on(PaymentSettledEvent event) { ... }
 * }</pre>
 * so they act only once the payment is committed, and write in a transaction of their own.
 *
 * <p><b>Errors:</b> the payment is final whatever a listener does. Listeners are idempotent (a repeated event changes
 * nothing) and log, rather than throw, when the reference is gone; Spring logs a listener's exception and does not
 * retry it.
 *
 * @param purpose      what was paid for
 * @param referenceId  the order or booking id
 * @param paymentId    the payment
 * @param farmerId     the farmer who paid
 * @param amountRupees the amount paid in whole rupees
 * @param settledAt    when the payment succeeded
 */
public record PaymentSettledEvent(
        PaymentPurpose purpose,
        long referenceId,
        long paymentId,
        long farmerId,
        long amountRupees,
        Instant settledAt) {
}
