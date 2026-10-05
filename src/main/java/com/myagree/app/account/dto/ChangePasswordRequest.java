package com.myagree.app.account.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/auth/me/password}; mirrors {@code ChangePasswordRequest} in frontend/src/lib/types.ts.
 *
 * @param newPassword 8-64 characters with at least one letter and one digit
 */
public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
}
