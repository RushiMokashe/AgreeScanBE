package com.myagree.app.account;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.Change;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.Role;

/**
 * Changes to an account for {@link AccountService#update}; a {@code null} field is left unchanged.
 *
 * @param email a change to the email address; a change to {@code null} or blank removes it
 * @param roles the complete new set of roles, at least one
 */
public record AccountUpdate(
        @Nullable String name,
        @Nullable Change<String> email,
        @Nullable Set<Role> roles,
        @Nullable Boolean active,
        @Nullable Language preferredLanguage) {

    /** The changes users may make to their own profile. */
    public static AccountUpdate profile(@Nullable String name, @Nullable Change<String> email,
                                        @Nullable Language preferredLanguage) {
        return new AccountUpdate(name, email, null, null, preferredLanguage);
    }
}
