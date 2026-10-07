package com.myagree.app.payment.dto;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.spi.PaymentPurpose;
import com.myagree.app.payment.PaymentProviderKind;
import com.myagree.app.payment.PaymentStatusCode;

/**
 * Mirrors {@code PaymentCheckout} in frontend/src/lib/types.ts: everything the payment page needs to collect the money.
 *
 * @param stripe   set when the provider is Stripe
 * @param razorpay set when the provider is Razorpay
 */
public record PaymentCheckoutResponse(
        long paymentId,
        PaymentPurpose purpose,
        long referenceId,
        long amount,
        String currency,
        String description,
        PaymentProviderKind provider,
        PaymentStatusCode status,
        @Nullable StripeCheckout stripe,
        @Nullable RazorpayCheckout razorpay) {

    /** Mirrors {@code StripeCheckout}. */
    public record StripeCheckout(String publishableKey, String clientSecret) {
    }

    /** Mirrors {@code RazorpayCheckout}; the amount is in paise. */
    public record RazorpayCheckout(
            String keyId,
            String orderId,
            long amountPaise,
            String currency,
            String merchantName,
            String description,
            String prefillName,
            String prefillContact) {
    }
}
