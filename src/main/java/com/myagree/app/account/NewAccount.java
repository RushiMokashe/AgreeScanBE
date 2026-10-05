package com.myagree.app.account;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.Role;

/**
 * What {@link AccountService#create} makes an account from.
 *
 * @param name              display name, at most {@value Account#NAME_MAX_LENGTH} characters
 * @param phone             10-digit Indian mobile number; a "+91" prefix, spaces and hyphens are accepted
 * @param email             optional; blank means none
 * @param roles             at least one
 * @param password          8-64 characters with at least one letter and one digit
 * @param preferredLanguage the language the apps start in
 */
public record NewAccount(
        String name,
        String phone,
        @Nullable String email,
        Set<Role> roles,
        String password,
        Language preferredLanguage) {
}
