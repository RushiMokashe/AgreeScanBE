package com.myagree.app.payment;

import org.jspecify.annotations.Nullable;

/**
 * A payment as its provider opened it.
 *
 * @param reference    the provider's id: a Stripe PaymentIntent, a Razorpay order or a simulated id
 * @param clientSecret what Stripe's Payment Element needs; {@code null} for the other providers
 */
record ProviderSession(String reference, @Nullable String clientSecret) {
}
