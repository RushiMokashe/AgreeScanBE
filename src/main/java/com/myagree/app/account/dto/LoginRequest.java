package com.myagree.app.account.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of {@code POST /api/auth/login}; mirrors {@code LoginRequest} in frontend/src/lib/types.ts. */
public record LoginRequest(@NotBlank String phone, @NotBlank String password) {
}
