package com.myagree.app.rental;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.spi.RentalHubDirectory;

/** Tells other features whether a rental hub exists, e.g. when a farmer saves a preferred hub. */
@Component
class RentalHubs implements RentalHubDirectory {

    private final RentalHubRepository hubRepository;

    RentalHubs(RentalHubRepository hubRepository) {
        this.hubRepository = hubRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(long hubId) {
        return hubRepository.existsById(hubId);
    }
}
