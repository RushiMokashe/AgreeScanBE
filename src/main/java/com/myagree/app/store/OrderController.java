package com.myagree.app.store;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.store.dto.OrderSummaryResponse;

/** The farmer's store orders. */
@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    List<OrderSummaryResponse> orders(CurrentUser user, Language language) {
        return orderService.orders(user.requireFarmerId(), language);
    }

    @GetMapping("/{id}")
    OrderSummaryResponse order(CurrentUser user, @PathVariable long id, Language language) {
        return orderService.order(user.requireFarmerId(), id, language);
    }
}
