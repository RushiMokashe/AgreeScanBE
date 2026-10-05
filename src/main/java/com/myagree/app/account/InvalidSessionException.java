package com.myagree.app.account;

import com.myagree.app.common.UnauthorizedException;
import com.myagree.app.common.i18n.UserMessage;

/** The refresh cookie cannot renew the session (missing, unknown, expired, reused or the account is deactivated). */
class InvalidSessionException extends UnauthorizedException {

    InvalidSessionException() {
        super(UserMessage.of("common.auth.session-ended"));
    }
}
