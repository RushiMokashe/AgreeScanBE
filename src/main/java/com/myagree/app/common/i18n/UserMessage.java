package com.myagree.app.common.i18n;

import java.util.List;

/**
 * Text for the user named by its code in the i18n message bundles, with the arguments that fill its placeholders
 * ({@code {0}}, {@code {1}}, ...). It is resolved with {@link Messages} in the reader's language when it is shown,
 * e.g. {@code throw new ConflictException(UserMessage.of("common.account.phone-taken", phone))}.
 *
 * @param code the message code, which starts with its bundle's namespace, e.g. "common.account.phone-taken"
 * @param args the placeholder values; numbers are formatted for the reader's language
 */
public record UserMessage(String code, List<Object> args) {

    public UserMessage {
        args = List.copyOf(args);
    }

    public static UserMessage of(String code, Object... args) {
        return new UserMessage(code, List.of(args));
    }
}
