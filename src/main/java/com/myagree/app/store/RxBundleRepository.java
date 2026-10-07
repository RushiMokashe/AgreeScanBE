package com.myagree.app.store;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RxBundleRepository extends JpaRepository<RxBundle, Long> {

    /** The farmer's latest prescription bundle. */
    @EntityGraph(attributePaths = "product")
    Optional<RxBundle> findFirstByFarmerIdOrderByPrescribedAtDesc(long farmerId);

    long countByFarmerId(long farmerId);
}
