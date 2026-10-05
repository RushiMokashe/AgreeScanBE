package com.myagree.app.common.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.NumberFormat;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

class LanguageTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "mr                              | MR",
            "hi-IN                           | HI",
            "EN-in                           | EN",
            "mr-IN,mr;q=0.9,en-US;q=0.8,en;q=0.7 | MR",
            "en;q=0.2, mr;q=0.8              | MR",
            "fr-FR, hi;q=0.5                 | HI",
            "de, fr;q=0.9                    | EN",
            "mr;q=0, hi;q=0.1                | HI",
            "*                               | EN"})
    void acceptLanguagePicksTheFavouriteLanguageAgriScanSpeaks(String header, Language expected) {
        assertThat(Language.fromAcceptLanguage(header)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "mr;q=abc", ";;;", "en-", "mr;q=0"})
    void missingMalformedOrRefusingHeadersGiveEnglish(String header) {
        assertThat(Language.fromAcceptLanguage(header)).isEqualTo(Language.DEFAULT).isEqualTo(Language.EN);
    }

    @Test
    void languageCodesAreTheFrontendsLanguageCodes() {
        assertThat(Language.EN.code()).isEqualTo("en");
        assertThat(Language.MR.code()).isEqualTo("mr");
        assertThat(Language.HI.code()).isEqualTo("hi");
        assertThat(Language.fromCode("MR")).isEqualTo(Language.MR);
        assertThatIllegalArgumentException().isThrownBy(() -> Language.fromCode("fr"));
    }

    @Test
    void localesAreIndianWithLatinDigits() {
        assertThat(Language.EN.locale().toLanguageTag()).isEqualTo("en-IN-u-nu-latn");
        assertThat(Language.MR.locale().toLanguageTag()).isEqualTo("mr-IN-u-nu-latn");
        assertThat(Language.HI.locale().toLanguageTag()).isEqualTo("hi-IN-u-nu-latn");
        // Marathi numbers default to Devanagari digits; AgriScan writes Latin digits in every language.
        assertThat(NumberFormat.getInstance(Locale.forLanguageTag("mr-IN")).format(2026)).isEqualTo("२,०२६");
        assertThat(NumberFormat.getInstance(Language.MR.locale()).format(2026)).isEqualTo("2,026");
    }

    @Test
    void jsonCarriesTheLanguageCode() {
        assertThat(jsonMapper.writeValueAsString(Language.HI)).isEqualTo("\"hi\"");
        assertThat(jsonMapper.readValue("\"mr\"", Language.class)).isEqualTo(Language.MR);
        assertThatThrownBy(() -> jsonMapper.readValue("\"fr\"", Language.class)).isInstanceOf(JacksonException.class);
    }
}
