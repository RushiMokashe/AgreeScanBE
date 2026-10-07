package com.myagree.app.store.dto;

import jakarta.validation.constraints.NotNull;

import com.myagree.app.store.PaymentMethod;

/** Mirrors {@code CheckoutRequest} in frontend/src/lib/types.ts. */
public record CheckoutRequest(@NotNull PaymentMethod paymentMethod) {
}
