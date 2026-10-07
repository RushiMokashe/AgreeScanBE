package com.myagree.app.common;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.Messages;
import com.myagree.app.common.i18n.UserMessage;

/**
 * A failure the API reports as an {@link ApiError} with this exception's HTTP status. The message for the user is
 * either fixed text or a {@link UserMessage}, which is looked up in the request's language only when the response is
 * written, so a service can fail without knowing the language.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final @Nullable UserMessage userMessage;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.userMessage = null;
    }

    protected ApiException(HttpStatus status, UserMessage message) {
        super(message.toString());
        this.status = status;
        this.userMessage = message;
    }

    public HttpStatus status() {
        return status;
    }

    /** The key of the user message, which a client may react to; {@code null} without one. */
    public @Nullable String code() {
        return userMessage != null ? userMessage.code() : null;
    }

    /** The explanation for the user, in {@code language} when it is a {@link UserMessage}. */
    public String userMessage(Messages messages, Language language) {
        return userMessage != null ? messages.get(userMessage, language) : getMessage();
    }
}
