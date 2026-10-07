package com.myagree.app.payment;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.payment.dto.ConfirmPaymentRequest;
import com.myagree.app.payment.dto.CreatePaymentRequest;
import com.myagree.app.payment.dto.PaymentCheckoutResponse;
import com.myagree.app.payment.dto.PaymentStatusResponse;

/** The farmer's online payments of orders and bookings. */
@RestController
@RequestMapping("/api/payments")
class PaymentController {

    private final PaymentService paymentService;

    PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PaymentCheckoutResponse create(CurrentUser user, @Valid @RequestBody CreatePaymentRequest request, Language language) {
        return paymentService.create(user, request, language);
    }

    @GetMapping("/{id}")
    PaymentStatusResponse status(CurrentUser user, @PathVariable long id, Language language) {
        return paymentService.status(user.requireFarmerId(), id, language);
    }

    @PostMapping("/{id}/confirm")
    PaymentStatusResponse confirm(CurrentUser user, @PathVariable long id,
                                  @RequestBody(required = false) @Valid @Nullable ConfirmPaymentRequest request,
                                  Language language) {
        return paymentService.confirm(user.requireFarmerId(), id, request != null ? request : ConfirmPaymentRequest.EMPTY,
                language);
    }
}
