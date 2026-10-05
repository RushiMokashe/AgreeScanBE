package com.myagree.app.common;

import org.springframework.http.HttpStatus;

import com.myagree.app.common.i18n.UserMessage;

/** The caller is known but not allowed to do this (deactivated account, missing profile, ...); answered with HTTP 403. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

    public ForbiddenException(UserMessage message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
