package com.myagree.app.common.spi;

import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.security.Role;

/**
 * Creates the profile a role needs when the admin portal creates an account ({@code POST /api/admin/users}): a FARMER
 * needs a farmer profile, a VEHICLE_OWNER a vehicle-owner profile.
 *
 * <p><b>Implemented by</b> the farm slice for {@link Role#FARMER} (with {@link FarmerProfileDetails}) and the rental
 * slice for {@link Role#VEHICLE_OWNER} (with {@link OwnerProfileDetails}). <b>Consumed by</b> the admin slice, which
 * injects {@code List<ProfileProvisioner>} and, right after {@code AccountService.create}, calls the provisioner of
 * each role the new account holds.
 *
 * <p><b>Contract:</b> creates the profile, links it to the account with {@code AccountService.linkFarmerProfile} or
 * {@code AccountService.linkOwnerProfile}, and returns its id. It joins the admin slice's transaction, so a failure
 * also undoes the new account. A {@link BadRequestException} reports invalid details, such as an unknown hub (the admin
 * slice answers 400); an {@link IllegalArgumentException} reports details of another role, a programming error.
 */
public interface ProfileProvisioner {

    /** The role whose profile this provisioner creates; exactly one provisioner serves each such role. */
    Role role();

    /**
     * @param userId  the new account's id
     * @param name    the account's name, which the profile shows
     * @param phone   the account's 10-digit mobile number
     * @param details the profile details for {@link #role()}
     * @return the id of the new farmer or vehicle-owner profile
     * @throws BadRequestException when the details are invalid
     */
    long provision(long userId, String name, String phone, ProfileDetails details);
}
