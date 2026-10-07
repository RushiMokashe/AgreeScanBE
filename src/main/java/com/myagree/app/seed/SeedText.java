package com.myagree.app.seed;

import com.myagree.app.common.i18n.LocalizedText;

/** Demo texts not translated yet: Marathi and Hindi readers see the English text until a translation is added. */
final class SeedText {

    private SeedText() {
    }

    static LocalizedText en(String english) {
        return LocalizedText.of(english, null, null);
    }
}
