package com.myagree.app.farmer;

import com.myagree.app.farmer.dto.FarmerPreferencesResponse;
import com.myagree.app.farmer.dto.FarmerProfileResponse;

final class FarmerMapper {

    private FarmerMapper() {
    }

    static FarmerProfileResponse toResponse(Farmer farmer) {
        return new FarmerProfileResponse(farmer.getId(), farmer.getName(), farmer.getLocation(), farmer.getSeason(),
                farmer.getAvatarUrl(),
                new FarmerPreferencesResponse(farmer.getPreferredRentalHubId(), farmer.getPreferredMandiMarketId()));
    }
}
