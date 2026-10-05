package com.myagree.app.store;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.store.dto.ProductResponse;
import com.myagree.app.store.dto.StoreHomeResponse;

@RestController
@RequestMapping("/api/store")
class StoreController {

    private final StoreService storeService;

    StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    StoreHomeResponse home() {
        return storeService.home();
    }

    @GetMapping("/products")
    List<ProductResponse> products(@RequestParam(required = false) @Nullable ProductCategory category,
                                   @RequestParam(required = false) @Nullable String q) {
        return storeService.searchProducts(category, q);
    }
}
