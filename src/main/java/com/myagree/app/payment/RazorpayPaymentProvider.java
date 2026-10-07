package com.myagree.app.payment;

import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.myagree.app.common.AgriScanProperties;
import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.payment.dto.ConfirmPaymentRequest;
import com.myagree.app.payment.dto.PaymentCheckoutResponse.RazorpayCheckout;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Razorpay (docs/architecture/phase-2.md, D5): an order in paise, paid in Razorpay Checkout (UPI, cards, netbanking,
 * wallets). The checkout's confirmation is verified with {@code HMAC_SHA256(orderId|paymentId, keySecret)}; the
 * {@code payment.captured}, {@code order.paid} and {@code payment.failed} webhooks with the webhook secret.
 */
@Component
class RazorpayPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentProvider.class);

    private static final long PAISE_PER_RUPEE = 100;
    private static final String MERCHANT_NAME = "AgriScan";
    private static final String RECEIPT_PREFIX = "agriscan-payment-";
    private static final String EVENT_CAPTURED = "payment.captured";
    private static final String EVENT_ORDER_PAID = "order.paid";
    private static final String EVENT_FAILED = "payment.failed";
    private static final String DECLINED = "Declined by Razorpay";
    private static final UserMessage CONFIRMATION_INVALID = UserMessage.of("payment.razorpay-confirmation-invalid");
    private static final UserMessage WEBHOOK_INVALID = UserMessage.of("payment.webhook-invalid");

    private final AgriScanProperties.Payments.Razorpay settings;
    private final @Nullable RazorpayClient client;

    RazorpayPaymentProvider(AgriScanProperties properties) throws RazorpayException {
        this.settings = properties.payments().razorpay();
        this.client = settings.isConfigured() ? new RazorpayClient(settings.keyId(), settings.keySecret()) : null;
    }

    @Override
    public PaymentProviderKind kind() {
        return PaymentProviderKind.RAZORPAY;
    }

    @Override
    public boolean isAvailable() {
        return client != null;
    }

    @Override
    public ProviderSession open(Payment payment) {
        JSONObject request = new JSONObject()
                .put("amount", payment.getAmountRupees() * PAISE_PER_RUPEE)
                .put("currency", Payment.CURRENCY)
                .put("receipt", RECEIPT_PREFIX + payment.getId())
                .put("notes", new JSONObject()
                        .put("paymentId", String.valueOf(payment.getId()))
                        .put("farmerId", String.valueOf(payment.getFarmerId())));
        try {
            Order order = Objects.requireNonNull(client, "Razorpay is not configured").orders.create(request);
            return new ProviderSession(order.get("id"), null);
        } catch (RazorpayException e) {
            log.warn("Razorpay refused to open payment {}: {}", payment.getId(), e.getMessage());
            throw new PaymentProviderException("Razorpay could not open payment " + payment.getId(), e);
        }
    }

    /**
     * A payment Razorpay Checkout reports paid, once its signature proves the report came from Razorpay.
     *
     * @throws BadRequestException when the confirmation is incomplete, for another order, or not signed by Razorpay
     */
    @Override
    public ProviderOutcome confirm(Payment payment, ConfirmPaymentRequest request) {
        String orderId = request.razorpayOrderId();
        String paymentId = request.razorpayPaymentId();
        String signature = request.razorpaySignature();
        if (orderId == null || paymentId == null || signature == null
                || !orderId.equals(payment.getProviderReference())
                || !HmacSignatures.matches(orderId + '|' + paymentId, settings.keySecret(), signature)) {
            throw new BadRequestException(CONFIRMATION_INVALID);
        }
        return ProviderOutcome.SUCCEEDED;
    }

    @Override
    public Optional<RazorpayCheckout> razorpayCheckout(Payment payment) {
        return Optional.of(new RazorpayCheckout(settings.keyId(), Objects.requireNonNull(payment.getProviderReference()),
                payment.getAmountRupees() * PAISE_PER_RUPEE, Payment.CURRENCY, MERCHANT_NAME, payment.getDescription(),
                payment.getPayerName(), payment.getPayerPhone()));
    }

    /**
     * The order and outcome a verified webhook reports; empty for events AgriScan does not act on.
     *
     * @throws BadRequestException when the signature does not verify or no webhook secret is configured
     */
    Optional<WebhookOutcome> readWebhook(String payload, @Nullable String signature) {
        if (signature == null || settings.webhookSecret().isBlank()
                || !HmacSignatures.matches(payload, settings.webhookSecret(), signature)) {
            throw new BadRequestException(WEBHOOK_INVALID);
        }
        JsonNode event;
        try {
            event = JsonMapper.shared().readTree(payload);
        } catch (JacksonException e) {
            throw new BadRequestException(WEBHOOK_INVALID);
        }
        JsonNode payment = event.path("payload").path("payment").path("entity");
        return switch (event.path("event").asString("")) {
            case EVENT_CAPTURED -> reference(payment.path("order_id")).map(order -> new WebhookOutcome(order, ProviderOutcome.SUCCEEDED));
            case EVENT_ORDER_PAID -> reference(event.path("payload").path("order").path("entity").path("id"))
                    .map(order -> new WebhookOutcome(order, ProviderOutcome.SUCCEEDED));
            case EVENT_FAILED -> reference(payment.path("order_id")).map(order -> new WebhookOutcome(order,
                    ProviderOutcome.failed(payment.path("error_description").asString(DECLINED))));
            default -> Optional.empty();
        };
    }

    private static Optional<String> reference(JsonNode node) {
        String value = node.asString("");
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
