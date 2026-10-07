package com.myagree.app.store;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.store.dto.AddCartItemRequest;
import com.myagree.app.store.dto.CartResponse;
import com.myagree.app.store.dto.CheckoutRequest;
import com.myagree.app.store.dto.OrderConfirmationResponse;

@RestController
@RequestMapping("/api/cart")
class CartController {

    private final CartService cartService;

    CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    CartResponse cart(Language language) {
        return cartService.currentCart(language);
    }

    @PostMapping("/items")
    CartResponse addItem(@Valid @RequestBody AddCartItemRequest request, Language language) {
        return cartService.addItem(request.productId(), request.quantity(), request.replacesCart(), language);
    }

    @DeleteMapping("/items/{itemId}")
    CartResponse removeItem(@PathVariable long itemId, Language language) {
        return cartService.removeItem(itemId, language);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    OrderConfirmationResponse checkout(@Valid @RequestBody CheckoutRequest request, Language language) {
        return cartService.checkout(request.paymentMethod(), language);
    }
}
