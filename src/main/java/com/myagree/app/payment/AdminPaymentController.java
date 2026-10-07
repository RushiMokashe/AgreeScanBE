package com.myagree.app.payment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.common.PageResponse;
import com.myagree.app.payment.dto.AdminPaymentResponse;

/** The admin portal's payments list; {@code /api/admin/**} is open to the ADMIN role only. */
@RestController
@RequestMapping("/api/admin/payments")
class AdminPaymentController {

    private static final int MAX_PAGE_SIZE = 100;

    private final PaymentService paymentService;

    AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    PageResponse<AdminPaymentResponse> payments(@RequestParam(required = false) @Nullable PaymentStatusCode status,
                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return paymentService.adminPayments(status, page, size);
    }
}
