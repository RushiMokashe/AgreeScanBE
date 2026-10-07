package com.myagree.app.store.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.i18n.LocalizedTextDto;
import com.myagree.app.store.Product;
import com.myagree.app.store.ProductCategory;

/**
 * Body of {@code POST /api/shop/products} and {@code PUT /api/shop/products/{id}}; mirrors {@code ShopProductInput} in
 * frontend/src/lib/types.ts. Texts are in English, with Marathi and Hindi when the shopkeeper adds them.
 *
 * @param packSize e.g. "500 g", "1 L"; {@code null} for none
 * @param mrp      the printed maximum retail price, at least the price; {@code null} for none
 * @param barcode  the 13-digit EAN-13 code on the pack, so farmers can scan it; {@code null} for none
 */
public record ShopProductRequest(
        @NotNull @Valid LocalizedTextDto name,
        @NotNull ProductCategory category,
        @Valid @Nullable LocalizedTextDto packSize,
        @NotNull @Valid LocalizedTextDto description,
        @Min(1) @Max(Product.MAX_PRICE) int price,
        @Min(1) @Max(Product.MAX_PRICE) @Nullable Integer mrp,
        boolean inStock,
        @Pattern(regexp = "\\d{13}") @Nullable String barcode) {
}
