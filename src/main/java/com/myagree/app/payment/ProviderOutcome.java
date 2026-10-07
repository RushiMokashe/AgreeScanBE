package com.myagree.app.payment;

import org.jspecify.annotations.Nullable;

/**
 * What a provider reports about a payment, from the client's confirmation or a webhook.
 *
 * @param status        {@code REQUIRES_PAYMENT} while nothing happened yet, else processing, succeeded or failed
 * @param failureReason the provider's explanation of a failure, e.g. "Card declined"
 */
record ProviderOutcome(PaymentStatusCode status, @Nullable String failureReason) {

    static final ProviderOutcome SUCCEEDED = new ProviderOutcome(PaymentStatusCode.SUCCEEDED, null);
    static final ProviderOutcome PROCESSING = new ProviderOutcome(PaymentStatusCode.PROCESSING, null);
    static final ProviderOutcome PENDING = new ProviderOutcome(PaymentStatusCode.REQUIRES_PAYMENT, null);

    static ProviderOutcome failed(String reason) {
        return new ProviderOutcome(PaymentStatusCode.FAILED, reason);
    }
}
