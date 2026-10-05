package com.myagree.app.common;

import org.springframework.http.HttpStatus;

import com.myagree.app.common.i18n.UserMessage;

/** The request clashes with the current state (phone number taken, removing your own access, ...); answered with HTTP 409. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    public ConflictException(UserMessage message) {
        super(HttpStatus.CONFLICT, message);
    }
}
