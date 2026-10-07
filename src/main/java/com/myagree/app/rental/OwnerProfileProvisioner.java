package com.myagree.app.rental;

import org.springframework.stereotype.Component;

import com.myagree.app.common.security.Role;
import com.myagree.app.common.spi.OwnerProfileDetails;
import com.myagree.app.common.spi.ProfileDetails;
import com.myagree.app.common.spi.ProfileProvisioner;

/** Creates the vehicle-owner profile of an account the admin portal creates with the VEHICLE_OWNER role. */
@Component
class OwnerProfileProvisioner implements ProfileProvisioner {

    private final VehicleOwnerService ownerService;

    OwnerProfileProvisioner(VehicleOwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @Override
    public Role role() {
        return Role.VEHICLE_OWNER;
    }

    @Override
    public long provision(long userId, String name, String phone, ProfileDetails details) {
        if (!(details instanceof OwnerProfileDetails owner)) {
            throw new IllegalArgumentException("A vehicle-owner profile needs owner details, not " + details);
        }
        return ownerService.createProfile(userId, name, owner.businessName(), phone, owner.hubId());
    }
}
