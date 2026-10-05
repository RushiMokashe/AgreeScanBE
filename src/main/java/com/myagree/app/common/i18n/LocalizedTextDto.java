package com.myagree.app.common.i18n;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * A {@link LocalizedText} as admin and owner endpoints send and receive it; mirrors {@code LocalizedText} in
 * frontend/src/lib/types.ts. Validate it where it is nested: {@code @NotNull @Valid LocalizedTextDto name}, or
 * {@code @Valid @Nullable LocalizedTextDto rateNote} for an optional text.
 */
public record LocalizedTextDto(
        @NotBlank @Size(max = LocalizedText.MAX_LENGTH) String en,
        @Size(max = LocalizedText.MAX_LENGTH) @Nullable String mr,
        @Size(max = LocalizedText.MAX_LENGTH) @Nullable String hi) {

    public static LocalizedTextDto from(LocalizedText text) {
        return new LocalizedTextDto(text.en(), text.mr(), text.hi());
    }

    public LocalizedText toLocalizedText() {
        return LocalizedText.of(en, mr, hi);
    }
}
