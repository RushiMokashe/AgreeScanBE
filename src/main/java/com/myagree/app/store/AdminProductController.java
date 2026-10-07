package com.myagree.app.store;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.store.dto.AdminProductResponse;
import com.myagree.app.store.dto.AdminProductUpdateRequest;

/** The admin portal's product prices; {@code /api/admin/**} is open to the ADMIN role only. */
@RestController
@RequestMapping("/api/admin/products")
class AdminProductController {

    private final StoreService storeService;

    AdminProductController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    List<AdminProductResponse> products(Language language) {
        return storeService.adminProducts(language);
    }

    @PatchMapping("/{id}")
    AdminProductResponse update(@PathVariable long id, @Valid @RequestBody AdminProductUpdateRequest update,
                                Language language) {
        return storeService.adminUpdate(id, update, language);
    }
}
