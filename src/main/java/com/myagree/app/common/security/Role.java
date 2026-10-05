package com.myagree.app.common.security;

/**
 * What a signed-in user may do; a user may hold several roles. Mirrors {@code Role} in frontend/src/lib/types.ts.
 * Lives with the security rules that enforce it, so {@code common} does not depend on the account feature.
 */
public enum Role {
    /** The farmer app: everything under {@code /api/**} except the admin and owner portals. */
    FARMER,
    /** The vehicle-owner portal under {@code /api/owner/**}. */
    VEHICLE_OWNER,
    /** The admin portal under {@code /api/admin/**}. */
    ADMIN
}
