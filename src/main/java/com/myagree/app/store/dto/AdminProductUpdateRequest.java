package com.myagree.app.store.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.myagree.app.store.Product;

/**
 * Body of {@code PATCH /api/admin/products/{id}}: each field given replaces the product's, the rest stay. An MRP sent
 * as {@code null} removes it, which differs from leaving it out; a record cannot tell the two apart, so this class
 * notes which fields the body named.
 */
public final class AdminProductUpdateRequest {

    @Min(1)
    @Max(Product.MAX_PRICE)
    private @Nullable Integer price;

    @Min(1)
    @Max(Product.MAX_PRICE)
    private @Nullable Integer mrp;

    private boolean mrpGiven;
    private @Nullable Boolean flashDeal;
    private @Nullable Boolean inStock;

    @JsonSetter
    void setPrice(@Nullable Integer price) {
        this.price = price;
    }

    @JsonSetter
    void setMrp(@Nullable Integer mrp) {
        this.mrp = mrp;
        this.mrpGiven = true;
    }

    @JsonSetter
    void setFlashDeal(@Nullable Boolean flashDeal) {
        this.flashDeal = flashDeal;
    }

    @JsonSetter
    void setInStock(@Nullable Boolean inStock) {
        this.inStock = inStock;
    }

    public @Nullable Integer price() {
        return price;
    }

    /** Whether the body named the MRP: then {@link #mrp()} is the new one, {@code null} for none. */
    public boolean mrpGiven() {
        return mrpGiven;
    }

    public @Nullable Integer mrp() {
        return mrp;
    }

    public @Nullable Boolean flashDeal() {
        return flashDeal;
    }

    public @Nullable Boolean inStock() {
        return inStock;
    }
}
