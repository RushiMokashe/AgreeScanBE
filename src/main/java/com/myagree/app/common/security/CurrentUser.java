package com.myagree.app.common.security;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.ForbiddenException;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.UserMessage;

/**
 * The signed-in user, as carried by the access token. Controllers declare a {@code CurrentUser} parameter;
 * services inject {@link CurrentUserProvider}.
 *
 * @param userId            the account id
 * @param name              display name when the token was issued
 * @param roles             what the user may do
 * @param farmerId          the user's farmer profile, if any
 * @param ownerId           the user's vehicle-owner profile, if any
 * @param preferredLanguage the account's preferred language when the token was issued (the request's language
 *                          comes from {@code Accept-Language} instead)
 */
public record CurrentUser(
        long userId,
        String name,
        Set<Role> roles,
        @Nullable Long farmerId,
        @Nullable Long ownerId,
        Language preferredLanguage) {

    private static final UserMessage NO_FARMER_PROFILE = UserMessage.of("common.account.no-farmer-profile");
    private static final UserMessage NO_OWNER_PROFILE = UserMessage.of("common.account.no-owner-profile");

    public CurrentUser {
        roles = Set.copyOf(roles);
    }

    /**
     * The farmer profile that farmer-owned data is scoped to.
     *
     * @throws ForbiddenException when the account has no farmer profile
     */
    public long requireFarmerId() {
        if (farmerId == null) {
            throw new ForbiddenException(NO_FARMER_PROFILE);
        }
        return farmerId;
    }

    /**
     * The vehicle-owner profile that owner-portal data is scoped to.
     *
     * @throws ForbiddenException when the account has no vehicle-owner profile
     */
    public long requireOwnerId() {
        if (ownerId == null) {
            throw new ForbiddenException(NO_OWNER_PROFILE);
        }
        return ownerId;
    }
}
