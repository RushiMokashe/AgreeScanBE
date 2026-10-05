package com.myagree.app.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

/**
 * Server-side display texts from the message bundles under {@code src/main/resources/i18n/}: one namespace per
 * feature, with {@code <ns>.properties} in English (the base bundle, and so the fallback for a missing translation)
 * plus {@code <ns>_mr.properties} and {@code <ns>_hi.properties}, all UTF-8. Codes read {@code <ns>.<area>.<name>},
 * e.g. {@code common.auth.bad-credentials}, so bundles never clash. Placeholders follow
 * {@link java.text.MessageFormat}: a message with arguments writes a literal apostrophe as {@code ''}.
 */
@Component
public class Messages {

    private final MessageSource messageSource;

    Messages(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * The message {@code code} in {@code language} with {@code args} filled in. A number argument is written with
     * Latin digits in every language but grouped in threes, so pass amounts formatted with {@link IndianNumbers} and
     * ids as strings (an order id 1234 would otherwise show as "1,234").
     *
     * @throws NoSuchMessageException when no bundle defines {@code code}
     */
    public String get(String code, Language language, Object... args) {
        return messageSource.getMessage(code, args, language.locale());
    }

    /** {@code message} in {@code language}; see {@link #get(String, Language, Object...)}. */
    public String get(UserMessage message, Language language) {
        return get(message.code(), language, message.args().toArray());
    }
}
