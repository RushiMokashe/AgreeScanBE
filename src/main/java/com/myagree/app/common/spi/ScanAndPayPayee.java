package com.myagree.app.common.spi;

import org.jspecify.annotations.Nullable;

/**
 * Who receives a Scan & Pay payment straight into their own bank account: the shopkeeper selling the order, or the
 * vehicle owner doing the job. The farmer scans their UPI QR (or opens their UPI ID in a UPI app), pays, and the payee
 * confirms the money arrived.
 *
 * <p>At least one of {@code upiId} and {@code qrImageUrl} is set; a seller with neither offers no Scan & Pay.
 *
 * @param payeeUserId the payee's account id; only this account may confirm or reject the payment
 * @param payeeName   the name the farmer sees, e.g. "Solapur Mandi Agro Depot"
 * @param upiId       the payee's UPI ID, e.g. "solapur.agro@okaxis", from which a QR with the amount filled in is drawn
 * @param qrImageUrl  the signed URL of the QR the payee uploaded (from PhonePe, Google Pay, ...), shown in preference
 */
public record ScanAndPayPayee(long payeeUserId, String payeeName, @Nullable String upiId, @Nullable String qrImageUrl) {

    public ScanAndPayPayee {
        if (upiId == null && qrImageUrl == null) {
            throw new IllegalArgumentException("A Scan & Pay payee has a UPI ID, a QR, or both");
        }
    }
}
