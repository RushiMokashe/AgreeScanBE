package com.myagree.app.payment;

import org.springframework.http.HttpStatus;

import com.myagree.app.common.ApiException;
import com.myagree.app.common.i18n.UserMessage;

/** A payment provider could not be reached or refused a request; answered with HTTP 502, the farmer tries again. */
class PaymentProviderException extends ApiException {

    private static final UserMessage PROVIDER_FAILED = UserMessage.of("payment.provider-failed");

    PaymentProviderException(String detail, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, PROVIDER_FAILED);
        initCause(new IllegalStateException(detail, cause));
    }
}
