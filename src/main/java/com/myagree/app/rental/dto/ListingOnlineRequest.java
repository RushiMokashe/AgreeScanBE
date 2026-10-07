package com.myagree.app.rental.dto;

import jakarta.validation.constraints.NotNull;

/** Body of {@code PATCH /api/owner/listings/{id}/online}. */
public record ListingOnlineRequest(@NotNull Boolean online) {
}
