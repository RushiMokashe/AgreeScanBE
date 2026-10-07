package com.myagree.app.payment.dto;

import java.time.Instant;

import com.myagree.app.common.spi.PaymentPurpose;
import com.myagree.app.payment.PaymentProviderKind;
import com.myagree.app.payment.PaymentStatusCode;

/** Mirrors {@code AdminPayment} in frontend/src/lib/types.ts; the payer's phone is their 10-digit number. */
public record AdminPaymentResponse(
        long id,
        PaymentPurpose purpose,
        long referenceId,
        String payerName,
        String payerPhone,
        long amount,
        PaymentProviderKind provider,
        PaymentStatusCode status,
        Instant createdAt,
        Instant updatedAt) {
}
