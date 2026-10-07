package com.myagree.app.store;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.store.dto.ShopProductRequest;
import com.myagree.app.store.dto.ShopProductResponse;
import com.myagree.app.store.dto.ShopStockRequest;

/** The shopkeeper portal's products: list, add, edit, stock and photo. */
@RestController
@RequestMapping("/api/shop/products")
class ShopProductController {

    private final ShopProductService productService;

    ShopProductController(ShopProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    List<ShopProductResponse> products(CurrentUser user) {
        return productService.products(user.requireShopId());
    }

    @GetMapping("/{id}")
    ShopProductResponse product(CurrentUser user, @PathVariable long id) {
        return productService.product(user.requireShopId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ShopProductResponse create(CurrentUser user, @Valid @RequestBody ShopProductRequest request) {
        return productService.create(user.requireShopId(), request);
    }

    @PutMapping("/{id}")
    ShopProductResponse update(CurrentUser user, @PathVariable long id, @Valid @RequestBody ShopProductRequest request) {
        return productService.update(user.requireShopId(), id, request);
    }

    @PatchMapping("/{id}/stock")
    ShopProductResponse changeStock(CurrentUser user, @PathVariable long id, @Valid @RequestBody ShopStockRequest request) {
        return productService.changeStock(user.requireShopId(), id, request.inStock());
    }

    @PostMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ShopProductResponse uploadPhoto(CurrentUser user, @PathVariable long id, @RequestPart("image") MultipartFile image) {
        return productService.uploadPhoto(user.requireShopId(), id, image);
    }
}
