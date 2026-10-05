package com.myagree.app.account;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.i18n.UserMessage;

/** Indian mobile numbers as accounts store them: ten digits starting with 6-9, e.g. "9876543210". */
final class PhoneNumbers {

    static final int LENGTH = 10;

    private static final UserMessage INVALID = UserMessage.of("common.account.phone-invalid");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s-]");
    /** The number, optionally preceded by India's country code with or without "+". */
    private static final Pattern INDIAN_MOBILE = Pattern.compile("(?:\\+?91)?([6-9]\\d{9})");

    private PhoneNumbers() {
    }

    /** "+91 98765-43210" gives "9876543210"; anything that is not an Indian mobile number gives empty. */
    static Optional<String> normalize(String input) {
        Matcher mobile = INDIAN_MOBILE.matcher(SEPARATORS.matcher(input).replaceAll(""));
        return mobile.matches() ? Optional.of(mobile.group(1)) : Optional.empty();
    }

    /**
     * @throws BadRequestException when {@code input} is not an Indian mobile number
     */
    static String require(String input) {
        return normalize(input).orElseThrow(() -> new BadRequestException(INVALID));
    }
}
