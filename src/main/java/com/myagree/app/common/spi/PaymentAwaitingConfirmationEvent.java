package com.myagree.app.common.spi;

import java.time.Instant;

/**
 * A farmer says they paid by Scan & Pay: the money went straight to the seller, who must now confirm it arrived. Until
 * then the order or booking shows "payment to verify" and cannot be paid again; the seller's decision follows as a
 * {@link PaymentSettledEvent} (received) or a {@link PaymentFailedEvent} (not received).
 *
 * <p><b>Published</b> and <b>consumed</b> like {@link PaymentSettledEvent}: by the payment slice when the farmer
 * submits the UPI transaction reference, and by the store and rental slices after commit, in a transaction of their
 * own. The consuming slice also tells its seller (the shopkeeper or the vehicle owner), who knows where in their
 * portal to confirm.
 *
 * @param purpose      what is paid for
 * @param referenceId  the order or booking id
 * @param paymentId    the payment the seller confirms or rejects ({@code POST /api/seller/payments/{id}/...})
 * @param farmerId     the farmer who paid
 * @param payerName    the farmer's name, for the seller's notification
 * @param amountRupees the amount in whole rupees
 * @param upiReference the 12-digit UPI transaction reference (UTR) the farmer's UPI app showed
 * @param submittedAt  when the farmer submitted it
 */
public record PaymentAwaitingConfirmationEvent(
        PaymentPurpose purpose,
        long referenceId,
        long paymentId,
        long farmerId,
        String payerName,
        long amountRupees,
        String upiReference,
        Instant submittedAt) {
}
