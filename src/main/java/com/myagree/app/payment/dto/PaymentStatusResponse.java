package com.myagree.app.payment.dto;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.spi.PaymentPurpose;
import com.myagree.app.payment.PaymentProviderKind;
import com.myagree.app.payment.PaymentStatusCode;

/** Mirrors {@code PaymentStatus} in frontend/src/lib/types.ts. */
public record PaymentStatusResponse(
        long paymentId,
        PaymentPurpose purpose,
        long referenceId,
        long amount,
        String description,
        PaymentProviderKind provider,
        PaymentStatusCode status,
        @Nullable String failureReason,
        Instant updatedAt) {
}
