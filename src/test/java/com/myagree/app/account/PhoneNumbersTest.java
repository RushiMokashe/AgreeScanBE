package com.myagree.app.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.myagree.app.common.BadRequestException;

class PhoneNumbersTest {

    @ParameterizedTest
    @ValueSource(strings = {"9876543210", "+919876543210", "919876543210", "+91 98765 43210", "98765-43210", " 98765 43210 "})
    void acceptsAnIndianMobileNumberWithCountryCodeSpacesOrHyphens(String input) {
        assertThat(PhoneNumbers.normalize(input)).contains("9876543210");
        assertThat(PhoneNumbers.require(input)).isEqualTo("9876543210");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12345", "5876543210", "98765432101", "+1 9876543210", "98765 4321O", "+91+9876543210"})
    void rejectsAnythingElse(String input) {
        assertThat(PhoneNumbers.normalize(input)).isEmpty();
        assertThatThrownBy(() -> PhoneNumbers.require(input)).isInstanceOf(BadRequestException.class);
    }
}
