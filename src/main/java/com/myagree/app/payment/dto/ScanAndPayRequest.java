package com.myagree.app.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import com.myagree.app.common.spi.PaymentPurpose;

/**
 * Body of {@code POST /api/payments/scan-and-pay}: the farmer paid the seller directly and quotes the UPI transaction
 * reference their UPI app showed. Mirrors {@code ScanAndPayRequest} in frontend/src/lib/types.ts.
 *
 * @param upiReference the 12-digit UPI transaction ID (UTR / RRN)
 */
public record ScanAndPayRequest(
        @NotNull PaymentPurpose purpose,
        @NotNull @Positive Long referenceId,
        @NotNull @Pattern(regexp = ScanAndPayRequest.UPI_REFERENCE_PATTERN) String upiReference) {

    public static final String UPI_REFERENCE_PATTERN = "\\d{12}";
}
