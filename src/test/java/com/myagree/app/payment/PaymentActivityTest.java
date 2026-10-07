package com.myagree.app.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.myagree.app.common.spi.PaymentPurpose;

/**
 * The documents the activity log writes against the schema MongoDB checks them with, without a MongoDB: every
 * required field present, no field the schema does not know, and the same allowed values on both sides.
 */
class PaymentActivityTest {

    private static final Instant AT = Instant.parse("2026-09-29T04:30:00Z");

    @Test
    void aFailedPaymentIsStoredWithEveryFieldTheSchemaRequiresAndItsReason() throws IOException {
        Document document = new PaymentActivity(7, PaymentActivity.Type.FAILED, PaymentActivity.Source.WEBHOOK,
                PaymentPurpose.STORE_ORDER, 12, 3, 730, PaymentProviderKind.RAZORPAY, "order_123", "Card declined", AT)
                .toDocument();

        assertThat(document.keySet())
                .containsAll(schema().getList("required", String.class))
                .isSubsetOf(schemaProperties().keySet());
        assertThat(document)
                .containsEntry("paymentId", 7L)
                .containsEntry("type", "FAILED")
                .containsEntry("source", "WEBHOOK")
                .containsEntry("amountRupees", 730L)
                .containsEntry("currency", "INR")
                .containsEntry("providerReference", "order_123")
                .containsEntry("failureReason", "Card declined")
                .containsEntry("occurredAt", Date.from(AT));
    }

    @Test
    void optionalFieldsAreLeftOutRatherThanStoredAsNull() {
        Document document = new PaymentActivity(7, PaymentActivity.Type.OPENED, PaymentActivity.Source.CHECKOUT,
                PaymentPurpose.RENTAL_BOOKING, 4, 3, 360, PaymentProviderKind.STRIPE, null, null, AT).toDocument();

        assertThat(document).doesNotContainKeys("providerReference", "failureReason");
    }

    @Test
    void theSchemaAllowsExactlyTheValuesTheAppWrites() throws IOException {
        assertThat(allowed("type")).containsExactlyElementsOf(names(PaymentActivity.Type.values()));
        assertThat(allowed("source")).containsExactlyElementsOf(names(PaymentActivity.Source.values()));
        assertThat(allowed("purpose")).containsExactlyElementsOf(names(PaymentPurpose.values()));
        assertThat(allowed("provider")).containsExactlyElementsOf(names(PaymentProviderKind.values()));
        assertThat(allowed("currency")).containsExactly(Payment.CURRENCY);
    }

    private static List<String> allowed(String property) throws IOException {
        return schemaProperties().get(property, Document.class).getList("enum", String.class);
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    private static Document schemaProperties() throws IOException {
        return schema().get("properties", Document.class);
    }

    private static Document schema() throws IOException {
        return Document.parse(new ClassPathResource(PaymentActivitySchema.SCHEMA).getContentAsString(StandardCharsets.UTF_8));
    }
}
