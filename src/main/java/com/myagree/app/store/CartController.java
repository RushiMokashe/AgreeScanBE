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

import com.myagree.app.store.dto.AddCartItemRequest;
import com.myagree.app.store.dto.CartResponse;
import com.myagree.app.store.dto.OrderConfirmationResponse;

@RestController
@RequestMapping("/api/cart")
class CartController {

    private final CartService cartService;

    CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    CartResponse cart() {
        return cartService.currentCart();
    }

    @PostMapping("/items")
    CartResponse addItem(@Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(request.productId(), request.quantity());
    }

    @DeleteMapping("/items/{itemId}")
    CartResponse removeItem(@PathVariable long itemId) {
        return cartService.removeItem(itemId);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    OrderConfirmationResponse checkout() {
        return cartService.checkout();
    }
}
