package com.myagree.app.store.dto;

/** Mirrors {@code CartItem} in frontend/src/lib/types.ts. */
public record CartItemResponse(
        long id,
        long productId,
        String name,
        String shortName,
        int quantity,
        int unitPrice,
        long lineTotal) {
}
