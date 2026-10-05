package com.myagree.app.notification;

import org.springframework.http.HttpStatus;

import com.myagree.app.common.ApiException;
import com.myagree.app.common.i18n.UserMessage;

/**
 * No notification stream can open right now: too many are open, or the server is shutting down. Answered with HTTP 503;
 * the client retries later and meanwhile refreshes its notifications by polling.
 */
class StreamUnavailableException extends ApiException {

    private static final UserMessage STREAM_UNAVAILABLE = UserMessage.of("notification.stream.unavailable");

    StreamUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, STREAM_UNAVAILABLE);
    }
}
