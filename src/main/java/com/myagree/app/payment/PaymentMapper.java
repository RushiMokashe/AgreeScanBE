package com.myagree.app.payment;

import com.myagree.app.payment.dto.AdminPaymentResponse;
import com.myagree.app.payment.dto.PaymentCheckoutResponse;
import com.myagree.app.payment.dto.PaymentStatusResponse;

final class PaymentMapper {

    private PaymentMapper() {
    }

    /**
     * The checkout of {@code payment}, with the part its provider's widget needs.
     *
     * @param description what the payment is for, in the reader's language
     */
    static PaymentCheckoutResponse toCheckout(Payment payment, PaymentProvider provider, String description) {
        return new PaymentCheckoutResponse(
                payment.getId(),
                payment.getPurpose(),
                payment.getReferenceId(),
                payment.getAmountRupees(),
                Payment.CURRENCY,
                description,
                payment.getProvider(),
                payment.getStatus(),
                provider.stripeCheckout(payment).orElse(null),
                provider.razorpayCheckout(payment).orElse(null));
    }

    /** @param description what the payment is for, in the reader's language */
    static PaymentStatusResponse toStatus(Payment payment, String description) {
        return new PaymentStatusResponse(
                payment.getId(),
                payment.getPurpose(),
                payment.getReferenceId(),
                payment.getAmountRupees(),
                description,
                payment.getProvider(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getUpdatedAt());
    }

    static AdminPaymentResponse toAdminResponse(Payment payment) {
        return new AdminPaymentResponse(
                payment.getId(),
                payment.getPurpose(),
                payment.getReferenceId(),
                payment.getPayerName(),
                payment.getPayerPhone(),
                payment.getAmountRupees(),
                payment.getProvider(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
