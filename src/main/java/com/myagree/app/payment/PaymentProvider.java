package com.myagree.app.payment;

import java.util.Optional;

import com.myagree.app.payment.dto.ConfirmPaymentRequest;
import com.myagree.app.payment.dto.PaymentCheckoutResponse.RazorpayCheckout;
import com.myagree.app.payment.dto.PaymentCheckoutResponse.StripeCheckout;

/**
 * One way of taking online payments (docs/architecture/phase-2.md, D5): Stripe, Razorpay or the simulated test
 * provider. A payment stays with the provider that opened it, even if the configured provider changes later.
 */
interface PaymentProvider {

    PaymentProviderKind kind();

    /** Whether its keys are configured, so it can take payments. */
    boolean isAvailable();

    /**
     * Opens the payment at the provider, idempotently per payment.
     *
     * @throws PaymentProviderException when the provider cannot be reached or refuses
     */
    ProviderSession open(Payment payment);

    /**
     * What the provider says about the payment now, given what the client sent after paying.
     *
     * @throws com.myagree.app.common.BadRequestException when the confirmation is missing or does not fit the payment
     * @throws PaymentProviderException                   when the provider cannot be reached
     */
    ProviderOutcome confirm(Payment payment, ConfirmPaymentRequest request);

    /** The Stripe part of the checkout; only the Stripe provider has one. */
    default Optional<StripeCheckout> stripeCheckout(Payment payment) {
        return Optional.empty();
    }

    /** The Razorpay part of the checkout; only the Razorpay provider has one. */
    default Optional<RazorpayCheckout> razorpayCheckout(Payment payment) {
        return Optional.empty();
    }
}
