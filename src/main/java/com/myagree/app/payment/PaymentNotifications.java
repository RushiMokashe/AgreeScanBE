package com.myagree.app.payment;

import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.myagree.app.common.spi.NotificationType;
import com.myagree.app.common.spi.Notifier;

/**
 * Tells the payer how their payment went (docs/architecture/phase-2.md, D10), linking to the order list or the paid
 * booking. Called inside the transaction that recorded the outcome; without the notification feature nobody is told.
 */
@Component
class PaymentNotifications {

    private static final String ORDERS_ROUTE = "/orders";
    private static final String BOOKING_ROUTE = "/bookings/";

    private final ObjectProvider<Notifier> notifiers;

    PaymentNotifications(ObjectProvider<Notifier> notifiers) {
        this.notifiers = notifiers;
    }

    void succeeded(Payment payment) {
        send(payment, NotificationType.PAYMENT_SUCCEEDED, Map.of(
                Notifier.AMOUNT, String.valueOf(payment.getAmountRupees()),
                Notifier.DESCRIPTION, payment.getDescription()));
    }

    void failed(Payment payment) {
        send(payment, NotificationType.PAYMENT_FAILED, Map.of(
                Notifier.AMOUNT, String.valueOf(payment.getAmountRupees()),
                Notifier.DESCRIPTION, payment.getDescription(),
                Notifier.REASON, Objects.requireNonNullElse(payment.getFailureReason(), "")));
    }

    private void send(Payment payment, NotificationType type, Map<String, String> params) {
        String route = switch (payment.getPurpose()) {
            case STORE_ORDER -> ORDERS_ROUTE;
            case RENTAL_BOOKING -> BOOKING_ROUTE + payment.getReferenceId();
        };
        notifiers.ifAvailable(notifier -> notifier.notify(payment.getPayerUserId(), type, params, route));
    }
}
