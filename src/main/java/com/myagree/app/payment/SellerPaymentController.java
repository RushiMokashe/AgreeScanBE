package com.myagree.app.payment;

import java.util.List;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.payment.dto.NotReceivedRequest;
import com.myagree.app.payment.dto.SellerPaymentResponse;

/**
 * Scan & Pay payments as their payee sees them: shopkeepers for their orders, vehicle owners for their bookings. The
 * money went straight to the seller, so only the seller can say whether it arrived.
 */
@RestController
@RequestMapping("/api/seller/payments")
class SellerPaymentController {

    private final PaymentService paymentService;

    SellerPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** Newest first; {@code status=PROCESSING} lists the payments still to confirm. */
    @GetMapping
    List<SellerPaymentResponse> payments(CurrentUser user, @RequestParam(required = false) @Nullable PaymentStatusCode status,
                                         Language language) {
        return paymentService.sellerPayments(user.userId(), status, language);
    }

    @PostMapping("/{id}/received")
    SellerPaymentResponse received(CurrentUser user, @PathVariable long id, Language language) {
        return paymentService.markReceived(user.userId(), id, language);
    }

    @PostMapping("/{id}/not-received")
    SellerPaymentResponse notReceived(CurrentUser user, @PathVariable long id,
                                      @RequestBody(required = false) @Valid @Nullable NotReceivedRequest request,
                                      Language language) {
        String reason = (request != null ? request : NotReceivedRequest.EMPTY).reason();
        return paymentService.markNotReceived(user.userId(), id, reason, language);
    }
}
