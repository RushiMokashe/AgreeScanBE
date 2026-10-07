package com.myagree.app.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.myagree.app.common.spi.PaymentPurpose;

/** Body of {@code POST /api/payments}; mirrors {@code CreatePaymentRequest} in frontend/src/lib/types.ts. */
public record CreatePaymentRequest(@NotNull PaymentPurpose purpose, @NotNull @Positive Long referenceId) {
}
