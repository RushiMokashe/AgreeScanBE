package com.myagree.app.account;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.myagree.app.common.BadRequestException;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"Farmer@123", "abcdefg1", "1234567a", "पासवर्ड2026"})
    void acceptsEightToSixtyFourCharactersWithALetterAndADigit(String password) {
        assertThatNoException().isThrownBy(() -> PasswordPolicy.check(password));
    }

    @Test
    void acceptsExactlySixtyFourCharacters() {
        assertThatNoException().isThrownBy(() -> PasswordPolicy.check("a1".repeat(32)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc1234", "abcdefgh", "12345678", "@@@@@@@@"})
    void rejectsShortPasswordsAndPasswordsWithoutALetterOrADigit(String password) {
        assertThatThrownBy(() -> PasswordPolicy.check(password)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsMoreThanSixtyFourCharacters() {
        assertThatThrownBy(() -> PasswordPolicy.check("a1".repeat(32) + "b")).isInstanceOf(BadRequestException.class);
    }

    /** BCrypt reads only 72 bytes, and 25 Devanagari letters already take 75 in UTF-8. */
    @Test
    void rejectsPasswordsThatBcryptWouldTruncate() {
        assertThatThrownBy(() -> PasswordPolicy.check("प".repeat(25) + "1")).isInstanceOf(BadRequestException.class);
    }
}
