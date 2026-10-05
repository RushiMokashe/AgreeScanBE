package com.myagree.app.store;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StoreMapperTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "0        | ₹0",
            "730      | ₹730",
            "2150     | ₹2,150",
            "108230   | ₹1,08,230",
            "10000000 | ₹1,00,00,000"
    })
    void rupeesUseIndianDigitGrouping(long amount, String expected) {
        assertThat(StoreMapper.rupees(amount)).isEqualTo(expected);
    }
}
