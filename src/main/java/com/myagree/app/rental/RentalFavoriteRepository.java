package com.myagree.app.rental;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RentalFavoriteRepository extends JpaRepository<RentalFavorite, Long> {

    @Query("select f.listing.id from RentalFavorite f where f.farmerId = :farmerId")
    Set<Long> findListingIdsByFarmerId(long farmerId);

    Optional<RentalFavorite> findByFarmerIdAndListingId(long farmerId, long listingId);
}
