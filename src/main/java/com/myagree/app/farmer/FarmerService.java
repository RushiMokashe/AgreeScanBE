package com.myagree.app.farmer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.ForbiddenException;
import com.myagree.app.common.NotFoundException;
import com.myagree.app.common.security.CurrentUserProvider;
import com.myagree.app.farmer.dto.FarmerProfileResponse;

@Service
@Transactional(readOnly = true)
public class FarmerService {

    private final FarmerRepository farmerRepository;
    private final CurrentUserProvider currentUserProvider;

    public FarmerService(FarmerRepository farmerRepository, CurrentUserProvider currentUserProvider) {
        this.farmerRepository = farmerRepository;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Profile of the signed-in farmer.
     *
     * @throws ForbiddenException when the signed-in account has no farmer profile
     */
    public FarmerProfileResponse currentFarmer() {
        long farmerId = currentUserProvider.get().requireFarmerId();
        return farmerRepository.findById(farmerId)
                .map(FarmerMapper::toResponse)
                .orElseThrow(() -> NotFoundException.of("Farmer", farmerId));
    }
}
