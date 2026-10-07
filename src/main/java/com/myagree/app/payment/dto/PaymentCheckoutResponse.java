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
        @Nullable RazorpayCheckout razorpay,
        @Nullable ScanAndPayOption scanAndPay) {

    /**
     * Paying the seller directly, offered when the shopkeeper or vehicle owner has set up Scan & Pay; mirrors
     * {@code ScanAndPayOption}.
     *
     * @param upiId        the seller's UPI ID, or {@code null} when they only uploaded a QR
     * @param upiLink      the UPI deep link with the amount filled in, opening a UPI app on the phone; with the UPI ID
     * @param qrImageUrl   the QR the seller uploaded (signed media URL), shown first when there is one
     * @param generatedQr  a QR of {@code upiLink} as a PNG data URL; with the UPI ID
     */
    public record ScanAndPayOption(
            String payeeName,
            @Nullable String upiId,
            @Nullable String upiLink,
            @Nullable String qrImageUrl,
            @Nullable String generatedQr) {
    }

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
