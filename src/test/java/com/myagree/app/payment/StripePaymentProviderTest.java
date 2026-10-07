package com.myagree.app.payment;

import static com.myagree.app.payment.PaymentProvidersTestSupport.STRIPE_WEBHOOK_SECRET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.myagree.app.common.BadRequestException;
import com.stripe.Stripe;

/** Stripe's webhook signatures, built the way Stripe signs them; no request reaches Stripe. */
class StripePaymentProviderTest {

    private static final String INTENT = "pi_3QabcDEF123";

    private final StripePaymentProvider provider = new StripePaymentProvider(PaymentProvidersTestSupport.properties());

    @Test
    void signedPaymentIntentEventsReportTheirOutcome() {
        String succeeded = event("payment_intent.succeeded", "succeeded", "null");
        String failed = event("payment_intent.payment_failed", "requires_payment_method",
                "{\"message\": \"Your card was declined.\"}");
        String processing = event("payment_intent.processing", "processing", "null");

        assertThat(provider.readWebhook(succeeded, signature(succeeded)))
                .contains(new WebhookOutcome(INTENT, ProviderOutcome.SUCCEEDED));
        assertThat(provider.readWebhook(failed, signature(failed)))
                .contains(new WebhookOutcome(INTENT, ProviderOutcome.failed("Your card was declined.")));
        assertThat(provider.readWebhook(processing, signature(processing)))
                .contains(new WebhookOutcome(INTENT, ProviderOutcome.PROCESSING));
    }

    @Test
    void otherEventsAreIgnoredAndForgedOnesRefused() {
        String created = event("payment_intent.created", "requires_payment_method", "null");

        assertThat(provider.readWebhook(created, signature(created))).isEmpty();
        assertThatThrownBy(() -> provider.readWebhook(created, "t=1,v1=" + "0".repeat(64)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> provider.readWebhook(created, null)).isInstanceOf(BadRequestException.class);
    }

    /** A PaymentIntent event in the API version this SDK reads. */
    private static String event(String type, String status, String lastPaymentError) {
        return """
                {"id": "evt_1", "object": "event", "api_version": "%s", "type": "%s",
                 "data": {"object": {"id": "%s", "object": "payment_intent", "status": "%s",
                 "last_payment_error": %s}}}""".formatted(Stripe.API_VERSION, type, INTENT, status, lastPaymentError);
    }

    /** The Stripe-Signature header: HMAC-SHA256 of "timestamp.payload" with the endpoint's secret. */
    private static String signature(String payload) {
        long timestamp = System.currentTimeMillis() / 1000;
        return "t=" + timestamp + ",v1=" + HmacSignatures.sign(timestamp + "." + payload, STRIPE_WEBHOOK_SECRET);
    }
}
