package com.myagree.app.payment;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.myagree.app.common.AgriScanProperties;
import com.myagree.app.common.AgriScanProperties.Payments.Provider;
import com.myagree.app.common.ApiException;
import com.myagree.app.common.i18n.UserMessage;

/**
 * Picks the provider new payments go to (docs/architecture/phase-2.md, D5) from {@code agriscan.payments.provider}:
 * {@code auto} takes Stripe when its keys are set, else Razorpay when its are, else the simulated provider. A provider
 * named explicitly without its keys takes no payments, rather than quietly falling back to test payments.
 */
@Component
class PaymentProviders {

    private static final Logger log = LoggerFactory.getLogger(PaymentProviders.class);
    private static final UserMessage UNAVAILABLE = UserMessage.of("payment.unavailable");

    private final Map<PaymentProviderKind, PaymentProvider> byKind = new EnumMap<>(PaymentProviderKind.class);
    private final PaymentProvider active;

    PaymentProviders(List<PaymentProvider> providers, AgriScanProperties properties) {
        providers.forEach(provider -> byKind.put(provider.kind(), provider));
        this.active = choose(properties.payments().provider());
        if (active.isAvailable()) {
            log.info("Online payments go to {}", active.kind());
        } else {
            log.warn("{} is chosen for online payments but its keys are not set: online payments are off", active.kind());
        }
    }

    /**
     * The provider that opens new payments.
     *
     * @throws ApiException with HTTP 503 when its keys are not configured
     */
    PaymentProvider active() {
        return available(active);
    }

    /** Whether the provider of {@code kind} has its keys, so its payments can still be paid. */
    boolean isAvailable(PaymentProviderKind kind) {
        return byKind.get(kind).isAvailable();
    }

    /** The provider that opened {@code payment}, which settles it whatever the configuration says now. */
    PaymentProvider of(Payment payment) {
        return available(byKind.get(payment.getProvider()));
    }

    StripePaymentProvider stripe() {
        return (StripePaymentProvider) byKind.get(PaymentProviderKind.STRIPE);
    }

    RazorpayPaymentProvider razorpay() {
        return (RazorpayPaymentProvider) byKind.get(PaymentProviderKind.RAZORPAY);
    }

    private PaymentProvider choose(Provider configured) {
        return switch (configured) {
            case STRIPE -> byKind.get(PaymentProviderKind.STRIPE);
            case RAZORPAY -> byKind.get(PaymentProviderKind.RAZORPAY);
            case SIMULATED -> byKind.get(PaymentProviderKind.SIMULATED);
            case AUTO -> stripe().isAvailable() ? stripe()
                    : razorpay().isAvailable() ? razorpay()
                    : byKind.get(PaymentProviderKind.SIMULATED);
        };
    }

    private static PaymentProvider available(PaymentProvider provider) {
        if (!provider.isAvailable()) {
            throw new ProvidersUnavailableException();
        }
        return provider;
    }

    /** The configured provider has no keys; answered with HTTP 503. */
    private static final class ProvidersUnavailableException extends ApiException {

        ProvidersUnavailableException() {
            super(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE);
        }
    }
}
