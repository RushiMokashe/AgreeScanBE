package com.myagree.app.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import com.myagree.app.account.Account;
import com.myagree.app.common.Change;
import com.myagree.app.common.i18n.Language;

/**
 * Body of {@code PATCH /api/auth/me}; mirrors {@code UpdateProfileRequest} in frontend/src/lib/types.ts.
 * Fields left out are unchanged.
 *
 * @param email {@code null} or blank removes the address
 */
public record UpdateProfileRequest(
        @Size(max = Account.NAME_MAX_LENGTH) @Pattern(regexp = UpdateProfileRequest.NOT_BLANK, message = "must not be blank")
        @Nullable String name,
        @Nullable Change<@Email @Size(max = Account.EMAIL_MAX_LENGTH) String> email,
        @Nullable Language preferredLanguage) {

    /** At least one character that is not whitespace, on any line. */
    static final String NOT_BLANK = "(?s).*\\S.*";
}
