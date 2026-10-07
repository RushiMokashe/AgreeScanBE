package com.myagree.app.payment.dto;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.spi.PaymentPurpose;
import com.myagree.app.payment.PaymentStatusCode;

/**
 * A Scan & Pay payment a farmer sent to the signed-in shopkeeper or vehicle owner ({@code GET /api/seller/payments}).
 * Mirrors {@code SellerPayment} in frontend/src/lib/types.ts.
 *
 * @param description  what it pays for, in the reader's language, e.g. "Agro Store order #12"
 * @param upiReference the UPI transaction reference to look for in the seller's bank statement
 * @param status       {@code PROCESSING} while it waits for the seller, then {@code SUCCEEDED} or {@code FAILED}
 * @param submittedAt  when the farmer said they paid
 */
public record SellerPaymentResponse(
        long paymentId,
        PaymentPurpose purpose,
        long referenceId,
        long amount,
        String description,
        String payerName,
        String payerPhone,
        String upiReference,
        PaymentStatusCode status,
        @Nullable String failureReason,
        Instant submittedAt,
        Instant updatedAt) {
}
