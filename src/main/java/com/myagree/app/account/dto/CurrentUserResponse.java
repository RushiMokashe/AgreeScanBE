package com.myagree.app.account.dto;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.Role;

/** Mirrors {@code CurrentUser} in frontend/src/lib/types.ts. */
public record CurrentUserResponse(
        long id,
        String name,
        String phone,
        @Nullable String email,
        List<Role> roles,
        Language preferredLanguage,
        @Nullable Long farmerId,
        @Nullable Long ownerId) {
}
