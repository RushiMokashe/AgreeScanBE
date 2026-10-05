package com.myagree.app.common.spi;

import java.time.Instant;

/**
 * A payment failed or was declined; nothing was charged, and the order or booking stays unpaid, so the farmer can pay
 * again.
 *
 * <p><b>Published</b> and <b>consumed</b> exactly like {@link PaymentSettledEvent}: by the payment slice when the
 * payment becomes {@code FAILED}, and by the store and rental slices after commit, in a transaction of their own. The
 * payment slice itself notifies the payer, so listeners usually only log it or release what they held for the payment.
 *
 * @param purpose      what the payment was for
 * @param referenceId  the order or booking id
 * @param paymentId    the payment
 * @param farmerId     the farmer who tried to pay
 * @param amountRupees the amount in whole rupees
 * @param failedAt     when the payment failed
 * @param reason       the provider's explanation, e.g. "Card declined", for logs and support
 */
public record PaymentFailedEvent(
        PaymentPurpose purpose,
        long referenceId,
        long paymentId,
        long farmerId,
        long amountRupees,
        Instant failedAt,
        String reason) {
}
