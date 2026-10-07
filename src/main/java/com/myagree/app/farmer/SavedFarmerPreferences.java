package com.myagree.app.farmer;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.spi.FarmerPreferencesReader;

/** The hub and market each farmer chose, for the rentals and mandi screens that open on them. */
@Component
class SavedFarmerPreferences implements FarmerPreferencesReader {

    private final FarmerRepository farmerRepository;

    SavedFarmerPreferences(FarmerRepository farmerRepository) {
        this.farmerRepository = farmerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> preferredRentalHub(long farmerId) {
        return farmerRepository.findById(farmerId).map(Farmer::getPreferredRentalHubId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> preferredMandiMarket(long farmerId) {
        return farmerRepository.findById(farmerId).map(Farmer::getPreferredMandiMarketId);
    }
}
