package com.myagree.app.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.UpiId;
import com.myagree.app.store.Shop;

/**
 * Body of {@code PUT /api/shop/me}; mirrors {@code ShopProfileInput} in frontend/src/lib/types.ts.
 *
 * @param upiId the UPI ID farmers pay by Scan & Pay, e.g. "solapur.agro@okaxis"; {@code null} or blank for none
 */
public record ShopProfileRequest(
        @NotBlank @Size(max = Shop.NAME_MAX_LENGTH) String name,
        @NotBlank @Size(max = Shop.PLACE_MAX_LENGTH) String place,
        @Size(max = UpiId.MAX_LENGTH) @Pattern(regexp = UpiId.PATTERN) @Nullable String upiId) {
}
