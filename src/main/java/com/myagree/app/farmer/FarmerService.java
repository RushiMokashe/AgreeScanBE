package com.myagree.app.farmer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.ForbiddenException;
import com.myagree.app.common.NotFoundException;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.security.CurrentUserProvider;
import com.myagree.app.common.spi.MandiMarketDirectory;
import com.myagree.app.common.spi.RentalHubDirectory;
import com.myagree.app.farmer.dto.FarmerPreferencesRequest;
import com.myagree.app.farmer.dto.FarmerProfileResponse;

@Service
@Transactional(readOnly = true)
public class FarmerService {

    private static final String UNKNOWN_HUB = "farm.preferences.unknown-hub";
    private static final String UNKNOWN_MARKET = "farm.preferences.unknown-market";

    private final FarmerRepository farmerRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectProvider<RentalHubDirectory> rentalHubs;
    private final ObjectProvider<MandiMarketDirectory> mandiMarkets;

    public FarmerService(FarmerRepository farmerRepository, CurrentUserProvider currentUserProvider,
                         ObjectProvider<RentalHubDirectory> rentalHubs, ObjectProvider<MandiMarketDirectory> mandiMarkets) {
        this.farmerRepository = farmerRepository;
        this.currentUserProvider = currentUserProvider;
        this.rentalHubs = rentalHubs;
        this.mandiMarkets = mandiMarkets;
    }

    /**
     * Profile of the signed-in farmer.
     *
     * @throws ForbiddenException when the signed-in account has no farmer profile
     */
    public FarmerProfileResponse currentFarmer() {
        return FarmerMapper.toResponse(findCurrentFarmer());
    }

    /**
     * Saves the hub and market the signed-in farmer chose; a choice left out keeps the saved one.
     *
     * @throws BadRequestException for a hub or market that does not exist
     */
    @Transactional
    public FarmerProfileResponse updatePreferences(FarmerPreferencesRequest request) {
        Farmer farmer = findCurrentFarmer();
        if (request.rentalHubId() != null) {
            long hubId = request.rentalHubId();
            RentalHubDirectory hubs = rentalHubs.getIfAvailable();
            require(hubs != null && hubs.exists(hubId), UNKNOWN_HUB, hubId);
            farmer.preferRentalHub(hubId);
        }
        if (request.mandiMarketId() != null) {
            long marketId = request.mandiMarketId();
            MandiMarketDirectory markets = mandiMarkets.getIfAvailable();
            require(markets != null && markets.exists(marketId), UNKNOWN_MARKET, marketId);
            farmer.preferMandiMarket(marketId);
        }
        return FarmerMapper.toResponse(farmer);
    }

    private Farmer findCurrentFarmer() {
        long farmerId = currentUserProvider.get().requireFarmerId();
        return farmerRepository.findById(farmerId).orElseThrow(() -> NotFoundException.of("Farmer", farmerId));
    }

    private static void require(boolean known, String code, long id) {
        if (!known) {
            throw new BadRequestException(UserMessage.of(code, String.valueOf(id)));
        }
    }
}
