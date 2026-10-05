package com.myagree.app.common;

import org.springframework.http.HttpStatus;

import com.myagree.app.common.i18n.UserMessage;

/** The caller could not be authenticated (wrong password, ended session, ...); answered with HTTP 401. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    public UnauthorizedException(UserMessage message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
