package com.myagree.app.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.UpiId;

import com.myagree.app.rental.VehicleOwner;

/** Body of {@code PUT /api/owner/me}; mirrors {@code OwnerProfileInput} in frontend/src/lib/types.ts. */
public record OwnerProfileRequest(
        @NotBlank @Size(max = VehicleOwner.NAME_MAX_LENGTH) String name,
        @Size(max = VehicleOwner.NAME_MAX_LENGTH) @Nullable String businessName,
        @NotNull Long hubId,
        @Size(max = UpiId.MAX_LENGTH) @Pattern(regexp = UpiId.PATTERN) @Nullable String upiId) {
}
