package com.myagree.app.rental.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.myagree.app.common.i18n.LocalizedTextDto;

/** Mirrors {@code OwnerFeature} in frontend/src/lib/types.ts; the icon is a Material Symbols name. */
public record OwnerFeatureDto(
        @NotNull @Pattern(regexp = OwnerListingRequest.ICON_PATTERN) String icon,
        @NotNull @Valid LocalizedTextDto text) {
}
