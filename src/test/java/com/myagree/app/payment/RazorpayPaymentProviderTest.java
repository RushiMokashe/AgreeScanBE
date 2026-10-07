package com.myagree.app.payment;

import static com.myagree.app.payment.PaymentProvidersTestSupport.RAZORPAY_KEY_SECRET;
import static com.myagree.app.payment.PaymentProvidersTestSupport.RAZORPAY_WEBHOOK_SECRET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.myagree.app.common.BadRequestException;
import com.myagree.app.payment.dto.ConfirmPaymentRequest;

/** Razorpay's signatures, checked the way its documentation computes them; no request reaches Razorpay. */
class RazorpayPaymentProviderTest {

    private static final String ORDER = "order_P1a2b3c4d5";
    private static final String RAZORPAY_PAYMENT = "pay_Q9z8y7x6w5";

    private final RazorpayPaymentProvider provider = newProvider();
    private final Payment payment = PaymentProvidersTestSupport.openedPayment(PaymentProviderKind.RAZORPAY, ORDER);

    @Test
    void aCheckoutSignedWithTheKeySecretSucceeds() {
        String signature = HmacSignatures.sign(ORDER + '|' + RAZORPAY_PAYMENT, RAZORPAY_KEY_SECRET);

        assertThat(provider.confirm(payment, confirmation(ORDER, RAZORPAY_PAYMENT, signature)))
                .isEqualTo(ProviderOutcome.SUCCEEDED);
        assertThat(provider.confirm(payment, confirmation(ORDER, RAZORPAY_PAYMENT, signature.toUpperCase())))
                .isEqualTo(ProviderOutcome.SUCCEEDED);
    }

    @Test
    void forgedIncompleteOrMisdirectedConfirmationsAreRefused() {
        String signature = HmacSignatures.sign(ORDER + '|' + RAZORPAY_PAYMENT, RAZORPAY_KEY_SECRET);
        String otherOrder = "order_Other00001";

        assertThatThrownBy(() -> provider.confirm(payment, confirmation(ORDER, RAZORPAY_PAYMENT, "0".repeat(64))))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> provider.confirm(payment, confirmation(otherOrder, RAZORPAY_PAYMENT,
                HmacSignatures.sign(otherOrder + '|' + RAZORPAY_PAYMENT, RAZORPAY_KEY_SECRET))))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> provider.confirm(payment, confirmation(ORDER, null, signature)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void signedWebhooksReportCapturedPaidAndFailedPayments() {
        String captured = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"id":"%s","order_id":"%s"}}}}"""
                .formatted(RAZORPAY_PAYMENT, ORDER);
        String paid = """
                {"event":"order.paid","payload":{"order":{"entity":{"id":"%s"}}}}""".formatted(ORDER);
        String failed = """
                {"event":"payment.failed","payload":{"payment":{"entity":{"order_id":"%s",
                "error_description":"Payment was declined by the bank"}}}}""".formatted(ORDER);

        assertThat(provider.readWebhook(captured, sign(captured)))
                .contains(new WebhookOutcome(ORDER, ProviderOutcome.SUCCEEDED));
        assertThat(provider.readWebhook(paid, sign(paid))).contains(new WebhookOutcome(ORDER, ProviderOutcome.SUCCEEDED));
        assertThat(provider.readWebhook(failed, sign(failed)))
                .contains(new WebhookOutcome(ORDER, ProviderOutcome.failed("Payment was declined by the bank")));
    }

    @Test
    void otherEventsAreIgnoredAndUnsignedOnesRefused() {
        String refund = """
                {"event":"refund.created","payload":{}}""";

        assertThat(provider.readWebhook(refund, sign(refund))).isEmpty();
        assertThatThrownBy(() -> provider.readWebhook(refund, HmacSignatures.sign(refund, "another secret")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> provider.readWebhook(refund, null)).isInstanceOf(BadRequestException.class);
    }

    private static String sign(String payload) {
        return HmacSignatures.sign(payload, RAZORPAY_WEBHOOK_SECRET);
    }

    private static ConfirmPaymentRequest confirmation(String order, String razorpayPayment, String signature) {
        return new ConfirmPaymentRequest(razorpayPayment, order, signature, null, null);
    }

    private static RazorpayPaymentProvider newProvider() {
        try {
            return new RazorpayPaymentProvider(PaymentProvidersTestSupport.properties());
        } catch (com.razorpay.RazorpayException e) {
            throw new IllegalStateException(e);
        }
    }
}
