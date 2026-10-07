package com.myagree.app.payment;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The providers' webhooks (docs/architecture/phase-2.md, D5): public, but only signed ones are read. The body is taken
 * as the exact text the provider signed. Answering 200 to a repeated or unknown event stops the provider retrying it.
 */
@RestController
@RequestMapping("/api/payments/webhooks")
class PaymentWebhookController {

    private final PaymentProviders providers;
    private final PaymentService paymentService;

    PaymentWebhookController(PaymentProviders providers, PaymentService paymentService) {
        this.providers = providers;
        this.paymentService = paymentService;
    }

    @PostMapping("/stripe")
    @ResponseStatus(HttpStatus.OK)
    void stripe(@RequestBody String payload, @RequestHeader(name = "Stripe-Signature", required = false) @Nullable String signature) {
        providers.stripe().readWebhook(payload, signature)
                .ifPresent(outcome -> paymentService.applyWebhook(PaymentProviderKind.STRIPE, outcome));
    }

    @PostMapping("/razorpay")
    @ResponseStatus(HttpStatus.OK)
    void razorpay(@RequestBody String payload,
                  @RequestHeader(name = "X-Razorpay-Signature", required = false) @Nullable String signature) {
        providers.razorpay().readWebhook(payload, signature)
                .ifPresent(outcome -> paymentService.applyWebhook(PaymentProviderKind.RAZORPAY, outcome));
    }
}
