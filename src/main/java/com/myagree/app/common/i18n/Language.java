package com.myagree.app.common.i18n;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The languages AgriScan speaks. In JSON a language is its {@code LanguageCode} from frontend/src/lib/types.ts
 * ("en", "mr", "hi"); controllers receive the request's language as a {@code Language} parameter.
 */
public enum Language {
    EN("en", "en-IN"),
    MR("mr", "mr-IN"),
    HI("hi", "hi-IN");

    /** Spoken when the client names no language, or only languages AgriScan does not offer. */
    public static final Language DEFAULT = EN;

    /** Numbers and dates use Latin digits in every language, as they do in the frontend. */
    private static final String LATIN_DIGITS_EXTENSION = "-u-nu-latn";
    private static final char SUBTAG_SEPARATOR = '-';

    private final String code;
    private final Locale locale;

    Language(String code, String languageTag) {
        this.code = code;
        this.locale = Locale.forLanguageTag(languageTag + LATIN_DIGITS_EXTENSION);
    }

    /** The {@code LanguageCode}, e.g. "mr". */
    @JsonValue
    public String code() {
        return code;
    }

    /** For formatting numbers and dates, e.g. mr-IN with Latin digits. */
    public Locale locale() {
        return locale;
    }

    /**
     * The language for a {@code LanguageCode}, ignoring case.
     *
     * @throws IllegalArgumentException for any other code (a JSON body with one is rejected with HTTP 400)
     */
    @JsonCreator
    public static Language fromCode(String code) {
        return findByCode(code).orElseThrow(() -> new IllegalArgumentException(
                "Unsupported language '%s'. Use one of %s".formatted(code, Arrays.stream(values()).map(Language::code).toList())));
    }

    /**
     * The client's most preferred language that AgriScan speaks, read leniently from an {@code Accept-Language}
     * header: quality values rank the entries, regions are ignored ("mr-IN" is Marathi), {@code q=0} excludes an
     * entry, and a missing, malformed or unsupported header gives {@link #DEFAULT}.
     */
    public static Language fromAcceptLanguage(@Nullable String header) {
        if (header == null || header.isBlank()) {
            return DEFAULT;
        }
        try {
            return Locale.LanguageRange.parse(header).stream()
                    .filter(range -> range.getWeight() > 0)
                    .map(range -> primaryLanguage(range.getRange()))
                    .flatMap(primary -> findByCode(primary).stream())
                    .findFirst()
                    .orElse(DEFAULT);
        } catch (IllegalArgumentException malformedHeader) {
            return DEFAULT;
        }
    }

    private static Optional<Language> findByCode(String code) {
        return Arrays.stream(values())
                .filter(language -> language.code.equalsIgnoreCase(code.strip()))
                .findFirst();
    }

    /** "mr" for "mr-in"; "*" stays "*" and matches nothing. */
    private static String primaryLanguage(String range) {
        int separator = range.indexOf(SUBTAG_SEPARATOR);
        return separator < 0 ? range : range.substring(0, separator);
    }
}
