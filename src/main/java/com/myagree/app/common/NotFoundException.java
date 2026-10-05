package com.myagree.app.common;

import org.springframework.http.HttpStatus;

import com.myagree.app.common.i18n.UserMessage;

/** A requested resource does not exist, or belongs to someone else; answered with HTTP 404. */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public NotFoundException(UserMessage message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    /** Builds the standard message, e.g. {@code of("Scan", 99)} gives "Scan 99 not found". */
    public static NotFoundException of(String resource, Object id) {
        return new NotFoundException("%s %s not found".formatted(resource, id));
    }
}
