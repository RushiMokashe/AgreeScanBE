package com.myagree.app.common;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * One page of a longer list; mirrors {@code PageResponse} in frontend/src/lib/types.ts.
 *
 * @param items      the entries on this page
 * @param page       zero-based page number
 * @param size       requested page size
 * @param totalItems entries across all pages
 * @param totalPages number of pages
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    /** Copies a Spring Data page; map its content first, e.g. {@code PageResponse.of(accounts.map(Mapper::toResponse))}. */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }
}
