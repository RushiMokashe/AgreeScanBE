package com.myagree.app.payment;

/**
 * What a verified provider webhook says about one payment.
 *
 * @param reference the provider's id of the payment: a Stripe PaymentIntent or a Razorpay order
 */
record WebhookOutcome(String reference, ProviderOutcome outcome) {
}
