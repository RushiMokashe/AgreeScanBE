package com.myagree.app.common;

import org.springframework.http.HttpStatus;

import com.myagree.app.common.i18n.UserMessage;

/** The request is well-formed but breaks a business rule (unknown crop, empty cart, ...); answered with HTTP 400. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public BadRequestException(UserMessage message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
