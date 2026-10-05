package com.myagree.app.common.i18n;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import org.jspecify.annotations.Nullable;

/**
 * Display text in English, Marathi and Hindi, stored with the entity that shows it. English is required;
 * a missing Marathi or Hindi text falls back to English. Values are stripped, and blank translations are stored
 * as {@code null}.
 *
 * <p>Each embedded text is stored in three columns named {@code <field>_en}, {@code <field>_mr} and
 * {@code <field>_hi}. Name them with Hibernate's {@code @EmbeddedColumnNaming}: the one-line form of the three
 * {@code @AttributeOverride}s ({@code en} to {@code title_en}, ...) that also keeps the column length declared here.
 * <pre>{@code
 * @Embedded
 * @EmbeddedColumnNaming("title_%s")      // org.hibernate.annotations: columns title_en, title_mr, title_hi
 * private LocalizedText title;
 *
 * @Embedded
 * @EmbeddedColumnNaming("rate_note_%s")  // an optional text: all three columns null reads back as null
 * private @Nullable LocalizedText rateNote;
 *
 * @Embeddable                             // texts inside an embeddable of an @ElementCollection, the same way
 * public record ListingSpec(@EmbeddedColumnNaming("value_%s") LocalizedText value,
 *                           @EmbeddedColumnNaming("label_%s") LocalizedText label) {
 * }
 * }</pre>
 * Mappers call {@link #resolve(Language)} with the request's language; admin and owner endpoints exchange the
 * three texts as {@link LocalizedTextDto}.
 *
 * @param en English text
 * @param mr Marathi text, or {@code null} to show English
 * @param hi Hindi text, or {@code null} to show English
 */
@Embeddable
public record LocalizedText(
        @Column(length = LocalizedText.MAX_LENGTH) String en,
        @Column(length = LocalizedText.MAX_LENGTH) @Nullable String mr,
        @Column(length = LocalizedText.MAX_LENGTH) @Nullable String hi) {

    /** Longest text stored per language. */
    public static final int MAX_LENGTH = 1000;

    public LocalizedText {
        if (en == null || en.isBlank()) {
            throw new IllegalArgumentException("A localized text needs its English version");
        }
        en = en.strip();
        mr = strippedOrNull(mr);
        hi = strippedOrNull(hi);
    }

    public static LocalizedText of(String en, @Nullable String mr, @Nullable String hi) {
        return new LocalizedText(en, mr, hi);
    }

    /** The text in {@code language}, or English when there is no translation. */
    public String resolve(Language language) {
        String translation = switch (language) {
            case EN -> en;
            case MR -> mr;
            case HI -> hi;
        };
        return translation != null ? translation : en;
    }

    private static @Nullable String strippedOrNull(@Nullable String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
