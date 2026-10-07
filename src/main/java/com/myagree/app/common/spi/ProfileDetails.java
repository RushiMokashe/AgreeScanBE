package com.myagree.app.common.spi;

/**
 * What a {@link ProfileProvisioner} creates a profile from; mirrors the {@code farmer}, {@code owner} and
 * {@code shop} parts of {@code CreateUserRequest} in frontend/src/lib/types.ts.
 */
public sealed interface ProfileDetails permits FarmerProfileDetails, OwnerProfileDetails, ShopProfileDetails {
}
