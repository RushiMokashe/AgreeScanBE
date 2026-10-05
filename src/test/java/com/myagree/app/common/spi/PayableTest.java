package com.myagree.app.common.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class PayableTest {

    @Test
    void somethingPayableCostsAtLeastOneRupee() {
        assertThat(payable(1).amountRupees()).isEqualTo(1);
        assertThatIllegalArgumentException().isThrownBy(() -> payable(0));
        assertThatIllegalArgumentException().isThrownBy(() -> payable(-730));
    }

    private static Payable payable(long amountRupees) {
        return new Payable(PaymentPurpose.STORE_ORDER, 12, 1, amountRupees, "Agro Store order #12", "Rishikesh",
                "9876543210");
    }
}
