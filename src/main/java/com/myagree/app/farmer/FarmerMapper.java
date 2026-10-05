package com.myagree.app.farmer;

import com.myagree.app.farmer.dto.FarmerPreferencesResponse;
import com.myagree.app.farmer.dto.FarmerProfileResponse;

final class FarmerMapper {

    /** Farmers cannot choose a rental hub or mandi market yet, so the defaults apply. */
    private static final FarmerPreferencesResponse DEFAULT_PREFERENCES = new FarmerPreferencesResponse(null, null);

    private FarmerMapper() {
    }

    static FarmerProfileResponse toResponse(Farmer farmer) {
        return new FarmerProfileResponse(farmer.getId(), farmer.getName(), farmer.getLocation(), farmer.getSeason(),
                farmer.getAvatarUrl(), DEFAULT_PREFERENCES);
    }
}
