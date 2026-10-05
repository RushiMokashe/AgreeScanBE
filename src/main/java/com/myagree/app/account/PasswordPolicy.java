package com.myagree.app.account;

import java.nio.charset.StandardCharsets;

import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.i18n.UserMessage;

/** Passwords are 8-64 characters long and contain at least one letter and one digit. */
final class PasswordPolicy {

    static final int MIN_LENGTH = 8;
    static final int MAX_LENGTH = 64;
    /** BCrypt reads at most 72 bytes, so longer passwords (possible with Devanagari letters) are refused. */
    private static final int MAX_BYTES = 72;

    private static final UserMessage WRONG_LENGTH = UserMessage.of("common.account.password-length", MIN_LENGTH, MAX_LENGTH);
    private static final UserMessage TOO_MANY_BYTES = UserMessage.of("common.account.password-too-many-bytes");
    private static final UserMessage LETTER_AND_DIGIT = UserMessage.of("common.account.password-letter-and-digit");

    private PasswordPolicy() {
    }

    /**
     * @throws BadRequestException naming the rule the password breaks
     */
    static void check(String password) {
        int length = password.codePointCount(0, password.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new BadRequestException(WRONG_LENGTH);
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new BadRequestException(TOO_MANY_BYTES);
        }
        if (password.codePoints().noneMatch(Character::isLetter) || password.codePoints().noneMatch(Character::isDigit)) {
            throw new BadRequestException(LETTER_AND_DIGIT);
        }
    }
}
