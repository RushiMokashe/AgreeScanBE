package com.myagree.app.store;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.CurrentUser;
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
    StoreHomeResponse home(CurrentUser user, Language language) {
        return storeService.home(user.requireFarmerId(), language);
    }

    @GetMapping("/products")
    List<ProductResponse> products(@RequestParam(required = false) @Nullable ProductCategory category,
                                   @RequestParam(required = false) @Nullable String q, Language language) {
        return storeService.searchProducts(category, q, language);
    }

    @GetMapping("/products/barcode/{code}")
    ProductResponse productByBarcode(@PathVariable String code, Language language) {
        return storeService.productByBarcode(code, language);
    }
}
