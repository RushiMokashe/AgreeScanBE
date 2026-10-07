package com.myagree.app.rental.dto;

import org.jspecify.annotations.Nullable;

/**
 * Mirrors {@code VehicleOwnerProfile} in frontend/src/lib/types.ts; the phone is the account's 10-digit number.
 *
 * @param upiId             where Scan & Pay sends farmers' payments; {@code null} for none
 * @param upiQrUrl          the UPI QR the owner uploaded, as a signed media URL; {@code null} for none
 * @param acceptsScanAndPay whether farmers can pay the owner directly
 */
public record VehicleOwnerProfileResponse(
        long id,
        long userId,
        String name,
        @Nullable String businessName,
        String phone,
        long hubId,
        String hubName,
        @Nullable String upiId,
        @Nullable String upiQrUrl,
        boolean acceptsScanAndPay) {
}
