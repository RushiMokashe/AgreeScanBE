package com.myagree.app.payment;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.myagree.app.common.AgriScanProperties;
import com.myagree.app.common.AgriScanProperties.Payments;
import com.myagree.app.common.spi.Payable;
import com.myagree.app.common.spi.PaymentPurpose;

/** Settings and payments for the provider tests, which never reach the network. */
final class PaymentProvidersTestSupport {

    static final String RAZORPAY_KEY_SECRET = "rzp_test_secret";
    static final String RAZORPAY_WEBHOOK_SECRET = "rzp_webhook_secret";
    static final String STRIPE_WEBHOOK_SECRET = "whsec_test_secret";

    private PaymentProvidersTestSupport() {
    }

    /** Both providers configured with test keys. */
    static AgriScanProperties properties() {
        return new AgriScanProperties(
                Path.of("target/test-uploads"),
                List.of(),
                new AgriScanProperties.Security("", false, Duration.ofMinutes(15), Duration.ofDays(30)),
                new Payments(Payments.Provider.AUTO,
                        new Payments.Stripe("sk_test_key", "pk_test_key", STRIPE_WEBHOOK_SECRET),
                        new Payments.Razorpay("rzp_test_key", RAZORPAY_KEY_SECRET, RAZORPAY_WEBHOOK_SECRET)),
                new AgriScanProperties.Mongodb(false, Duration.ofSeconds(5), Duration.ofSeconds(5), 20),
                new AgriScanProperties.Assistant(new AgriScanProperties.Assistant.Anthropic("", "claude-opus-5-5",
                        Duration.ofSeconds(15))));
    }

    /** A ₹730 store order payment opened at {@code provider} as {@code reference}. */
    static Payment openedPayment(PaymentProviderKind provider, String reference) {
        Payment payment = new Payment(new Payable(PaymentPurpose.STORE_ORDER, 12, 3, 730, "Agro Store order #12",
                "Rishikesh", "9876543210", null), 2, provider, Instant.parse("2026-09-29T04:30:00Z"));
        payment.opened(new ProviderSession(reference, null));
        return payment;
    }
}
