package com.myagree.app.common.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.StaticMessageSource;

class MessagesTest {

    private final StaticMessageSource bundles = new StaticMessageSource();
    private final Messages messages = new Messages(bundles);

    @Test
    void resolvesTheMessageInTheRequestedLanguage() {
        bundles.addMessage("store.cart.total", Language.EN.locale(), "Total {0}");
        bundles.addMessage("store.cart.total", Language.MR.locale(), "एकूण {0}");

        assertThat(messages.get("store.cart.total", Language.EN, 730)).isEqualTo("Total 730");
        assertThat(messages.get("store.cart.total", Language.MR, 730)).isEqualTo("एकूण 730");
    }

    @Test
    void numbersAreWrittenWithLatinDigitsAndAmountsArePassedFormatted() {
        bundles.addMessage("store.order.placed", Language.MR.locale(), "ऑर्डर #{0}: {1} वस्तू, एकूण {2}");

        assertThat(messages.get("store.order.placed", Language.MR, "1234", 2026, IndianNumbers.rupees(108230)))
                .isEqualTo("ऑर्डर #1234: 2,026 वस्तू, एकूण ₹1,08,230");
    }

    @Test
    void userMessagesResolveTheSameWay() {
        bundles.addMessage("common.account.phone-taken", Language.HI.locale(), "फ़ोन नंबर {0} पहले से पंजीकृत है");

        assertThat(messages.get(UserMessage.of("common.account.phone-taken", "9876543210"), Language.HI))
                .isEqualTo("फ़ोन नंबर 9876543210 पहले से पंजीकृत है");
    }

    @Test
    void unknownCodeIsAnError() {
        assertThatThrownBy(() -> messages.get("store.no-such-message", Language.MR))
                .isInstanceOf(NoSuchMessageException.class);
    }
}
