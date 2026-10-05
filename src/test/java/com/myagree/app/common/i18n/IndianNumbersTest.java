package com.myagree.app.common.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class IndianNumbersTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0                    | 0",
            "730                  | 730",
            "8450                 | 8,450",
            "108230               | 1,08,230",
            "1234567              | 12,34,567",
            "12345678             | 1,23,45,678",
            "-1234567             | -12,34,567",
            "9223372036854775807  | 92,23,37,20,36,85,47,75,807",
            "-9223372036854775808 | -92,23,37,20,36,85,47,75,808"})
    void groupsTheLastThreeDigitsThenPairs(long value, String expected) {
        assertThat(IndianNumbers.format(value)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"420 | ₹420", "108230 | ₹1,08,230", "-500 | -₹500", "-150000 | -₹1,50,000"})
    void writesRupeesTheWayTheFrontendDoes(long amount, String expected) {
        assertThat(IndianNumbers.rupees(amount)).isEqualTo(expected);
    }
}
