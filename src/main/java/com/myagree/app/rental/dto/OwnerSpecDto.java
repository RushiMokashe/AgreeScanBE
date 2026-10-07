package com.myagree.app.rental.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.myagree.app.common.i18n.LocalizedTextDto;

/** Mirrors {@code OwnerSpec} in frontend/src/lib/types.ts: a spec tile in all three languages. */
public record OwnerSpecDto(@NotNull @Valid LocalizedTextDto value, @NotNull @Valid LocalizedTextDto label) {
}
