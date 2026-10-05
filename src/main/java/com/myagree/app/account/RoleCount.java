package com.myagree.app.account;

import com.myagree.app.common.security.Role;

/** How many accounts hold one role; a row of {@link UserRepository#countUsersByRole()}. */
public record RoleCount(Role role, long users) {
}
