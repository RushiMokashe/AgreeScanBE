package com.myagree.app.payment;

import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.myagree.app.common.AgriScanProperties;
import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.payment.dto.ConfirmPaymentRequest;
import com.myagree.app.payment.dto.PaymentCheckoutResponse.StripeCheckout;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeError;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;

/**
 * Stripe (docs/architecture/phase-2.md, D5): a PaymentIntent in paise with automatic payment methods, so the Payment
 * Element shows cards and, on India-enabled accounts, UPI. Payments are confirmed by asking Stripe for the intent, and
 * settled by the {@code payment_intent.*} webhooks, verified with the webhook secret.
 */
@Component
class StripePaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentProvider.class);

    private static final long PAISE_PER_RUPEE = 100;
    private static final String CURRENCY = "inr";
    private static final String IDEMPOTENCY_PREFIX = "agriscan-payment-";
    private static final String SUCCEEDED = "succeeded";
    private static final String PROCESSING = "processing";
    private static final String CANCELED = "canceled";
    private static final String REQUIRES_PAYMENT_METHOD = "requires_payment_method";
    private static final String EVENT_SUCCEEDED = "payment_intent.succeeded";
    private static final String EVENT_FAILED = "payment_intent.payment_failed";
    private static final String EVENT_PROCESSING = "payment_intent.processing";
    private static final String DECLINED = "Declined by Stripe";
    private static final String CANCELLED = "Cancelled at Stripe";
    private static final UserMessage WEBHOOK_INVALID = UserMessage.of("payment.webhook-invalid");

    private final AgriScanProperties.Payments.Stripe settings;
    private final @Nullable StripeClient client;

    StripePaymentProvider(AgriScanProperties properties) {
        this.settings = properties.payments().stripe();
        this.client = settings.isConfigured() ? new StripeClient(settings.secretKey()) : null;
    }

    @Override
    public PaymentProviderKind kind() {
        return PaymentProviderKind.STRIPE;
    }

    @Override
    public boolean isAvailable() {
        return client != null;
    }

    @Override
    public ProviderSession open(Payment payment) {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(payment.getAmountRupees() * PAISE_PER_RUPEE)
                .setCurrency(CURRENCY)
                .setDescription(payment.getDescription())
                .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                .putMetadata("paymentId", String.valueOf(payment.getId()))
                .putMetadata("farmerId", String.valueOf(payment.getFarmerId()))
                .build();
        RequestOptions options = RequestOptions.builder().setIdempotencyKey(IDEMPOTENCY_PREFIX + payment.getId()).build();
        try {
            PaymentIntent intent = client().v1().paymentIntents().create(params, options);
            return new ProviderSession(intent.getId(), intent.getClientSecret());
        } catch (StripeException e) {
            log.warn("Stripe refused to open payment {}: {}", payment.getId(), e.getMessage());
            throw new PaymentProviderException("Stripe could not open payment " + payment.getId(), e);
        }
    }

    /** The intent's state at Stripe; the browser's confirmation carries nothing Stripe has not already got. */
    @Override
    public ProviderOutcome confirm(Payment payment, ConfirmPaymentRequest request) {
        try {
            return outcomeOf(client().v1().paymentIntents().retrieve(Objects.requireNonNull(payment.getProviderReference())));
        } catch (StripeException e) {
            log.warn("Stripe could not tell the state of payment {}: {}", payment.getId(), e.getMessage());
            throw new PaymentProviderException("Stripe could not confirm payment " + payment.getId(), e);
        }
    }

    @Override
    public Optional<StripeCheckout> stripeCheckout(Payment payment) {
        return Optional.of(new StripeCheckout(settings.publishableKey(), Objects.requireNonNull(payment.getClientSecret())));
    }

    /**
     * The PaymentIntent and outcome a verified webhook reports; empty for events AgriScan does not act on.
     *
     * @throws BadRequestException when the signature does not verify or no webhook secret is configured
     */
    Optional<WebhookOutcome> readWebhook(String payload, @Nullable String signatureHeader) {
        if (signatureHeader == null || settings.webhookSecret().isBlank()) {
            throw new BadRequestException(WEBHOOK_INVALID);
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, settings.webhookSecret());
        } catch (SignatureVerificationException e) {
            throw new BadRequestException(WEBHOOK_INVALID);
        }
        if (!(event.getDataObjectDeserializer().getObject().orElse(null) instanceof PaymentIntent intent)) {
            return Optional.empty();
        }
        return switch (event.getType()) {
            case EVENT_SUCCEEDED, EVENT_FAILED, EVENT_PROCESSING -> Optional.of(new WebhookOutcome(intent.getId(), outcomeOf(intent)));
            default -> Optional.empty();
        };
    }

    private static ProviderOutcome outcomeOf(PaymentIntent intent) {
        StripeError error = intent.getLastPaymentError();
        return switch (intent.getStatus()) {
            case SUCCEEDED -> ProviderOutcome.SUCCEEDED;
            case PROCESSING -> ProviderOutcome.PROCESSING;
            case CANCELED -> ProviderOutcome.failed(CANCELLED);
            case REQUIRES_PAYMENT_METHOD -> error != null
                    ? ProviderOutcome.failed(Objects.requireNonNullElse(error.getMessage(), DECLINED))
                    : ProviderOutcome.PENDING;
            default -> ProviderOutcome.PENDING;
        };
    }

    private StripeClient client() {
        return Objects.requireNonNull(client, "Stripe is not configured");
    }
}
