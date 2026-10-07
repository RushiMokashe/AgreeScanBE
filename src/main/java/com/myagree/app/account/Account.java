package com.myagree.app.account;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.common.security.Role;

/**
 * A user account as other features see it; mirrors {@code AdminUser} in frontend/src/lib/types.ts.
 *
 * @param phone     10-digit mobile number, e.g. "9876543210"
 * @param roles     in declaration order of {@link Role}
 * @param farmerId  the linked farmer profile, if any
 * @param ownerId   the linked vehicle-owner profile, if any
 */
public record Account(
        long id,
        String name,
        String phone,
        @Nullable String email,
        List<Role> roles,
        boolean active,
        Language preferredLanguage,
        @Nullable Long farmerId,
        @Nullable Long ownerId,
        @Nullable Long shopId,
        Instant createdAt,
        @Nullable Instant lastLoginAt) {

    public static final int NAME_MAX_LENGTH = 60;
    public static final int EMAIL_MAX_LENGTH = 120;

    public Account {
        roles = List.copyOf(roles);
    }

    /** The account as the signed-in user its access tokens describe. */
    public CurrentUser toCurrentUser() {
        return new CurrentUser(id, name, Set.copyOf(roles), farmerId, ownerId, shopId, preferredLanguage);
    }
}
