package com.myagree.app.store.dto;

import org.jspecify.annotations.Nullable;

/**
 * The signed-in shopkeeper's shop ({@code GET /api/shop/me}); mirrors {@code ShopProfile} in frontend/src/lib/types.ts.
 *
 * @param upiQrUrl          the UPI QR the shopkeeper uploaded, as a signed media URL; {@code null} for none
 * @param acceptsScanAndPay whether farmers can pay the shop directly: it has a UPI ID or a QR
 */
public record ShopProfileResponse(
        long id,
        String name,
        String place,
        String phone,
        @Nullable String upiId,
        @Nullable String upiQrUrl,
        boolean acceptsScanAndPay) {
}
