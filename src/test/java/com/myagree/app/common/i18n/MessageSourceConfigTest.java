package com.myagree.app.common.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.context.NoSuchMessageException;

class MessageSourceConfigTest {

    private Messages messages;

    /** A namespace whose feature has no texts yet, listed before one that has them. */
    @BeforeEach
    void configureBundles() throws IOException {
        MessageSourceProperties properties = new MessageSourceProperties();
        properties.setBasename(List.of("i18n/not-written-yet", "i18n/common"));
        properties.setEncoding(StandardCharsets.UTF_8);
        properties.setFallbackToSystemLocale(false);
        messages = new Messages(new MessageSourceConfig().messageSource(properties));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "EN | Incorrect phone number or password",
            "MR | फोन नंबर किंवा पासवर्ड चुकीचा आहे",
            "HI | फ़ोन नंबर या पासवर्ड गलत है"})
    void aNamespaceWithoutBundlesIsSkippedInEveryLanguage(Language language, String expected) {
        assertThat(messages.get("common.auth.bad-credentials", language)).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void anUnknownCodeIsStillReportedAsSuch(Language language) {
        assertThatThrownBy(() -> messages.get("farm.no-such-message", language)).isInstanceOf(NoSuchMessageException.class);
    }
}
