package com.myagree.app.store;

import org.springframework.stereotype.Component;

import com.myagree.app.common.security.Role;
import com.myagree.app.common.spi.ProfileDetails;
import com.myagree.app.common.spi.ProfileProvisioner;
import com.myagree.app.common.spi.ShopProfileDetails;

/** Opens the shop of a new SHOPKEEPER account the admin portal creates. */
@Component
class ShopProvisioner implements ProfileProvisioner {

    private final ShopService shopService;

    ShopProvisioner(ShopService shopService) {
        this.shopService = shopService;
    }

    @Override
    public Role role() {
        return Role.SHOPKEEPER;
    }

    @Override
    public long provision(long userId, String name, String phone, ProfileDetails details) {
        if (!(details instanceof ShopProfileDetails shop)) {
            throw new IllegalArgumentException("A shop needs shop details, not " + details);
        }
        return shopService.open(userId, shop.shopName().strip(), shop.place().strip(), phone).getId();
    }
}
