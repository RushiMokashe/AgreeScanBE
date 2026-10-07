package com.myagree.app.payment;

import java.time.Instant;
import java.util.Date;

import org.bson.Document;
import org.jspecify.annotations.Nullable;

import com.myagree.app.common.spi.PaymentPurpose;

/**
 * One change in a payment's life, published by {@link PaymentService} and kept in MongoDB by
 * {@link PaymentActivityLog}: one document in collection {@value #COLLECTION}, whose schema is
 * {@code mongodb/payment_events.schema.json}.
 *
 * @param providerReference the provider's id for the payment, once it was opened there
 * @param failureReason     why it failed, only for {@link Type#FAILED}
 */
record PaymentActivity(
        long paymentId,
        Type type,
        Source source,
        PaymentPurpose purpose,
        long referenceId,
        long farmerId,
        long amountRupees,
        PaymentProviderKind provider,
        @Nullable String providerReference,
        @Nullable String failureReason,
        Instant occurredAt) {

    static final String COLLECTION = "payment_events";

    /** What happened; the payment's status afterwards has the same name, except for {@link #OPENED}. */
    enum Type {
        OPENED,
        PROCESSING,
        SUCCEEDED,
        FAILED,
        CANCELLED
    }

    /** Who reported it. */
    enum Source {
        /** The farmer started checkout: a payment was opened, or replaced the one it cancelled. */
        CHECKOUT,
        /** The app confirmed with the provider after the farmer paid. */
        CONFIRMATION,
        /** The provider's webhook. */
        WEBHOOK
    }

    /** {@code payment} as it is right after {@code type} happened. */
    static PaymentActivity of(Payment payment, Type type, Source source, Instant at) {
        return new PaymentActivity(payment.getId(), type, source, payment.getPurpose(), payment.getReferenceId(),
                payment.getFarmerId(), payment.getAmountRupees(), payment.getProvider(), payment.getProviderReference(),
                type == Type.FAILED ? payment.getFailureReason() : null, at);
    }

    /** The document stored in MongoDB; optional fields are left out rather than stored as null. */
    Document toDocument() {
        Document document = new Document()
                .append("paymentId", paymentId)
                .append("type", type.name())
                .append("source", source.name())
                .append("purpose", purpose.name())
                .append("referenceId", referenceId)
                .append("farmerId", farmerId)
                .append("amountRupees", amountRupees)
                .append("currency", Payment.CURRENCY)
                .append("provider", provider.name());
        if (providerReference != null) {
            document.append("providerReference", providerReference);
        }
        if (failureReason != null) {
            document.append("failureReason", failureReason);
        }
        return document.append("occurredAt", Date.from(occurredAt));
    }
}
