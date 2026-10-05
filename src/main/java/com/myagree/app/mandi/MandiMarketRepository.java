package com.myagree.app.mandi;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MandiMarketRepository extends JpaRepository<MandiMarket, Long> {

    Optional<MandiMarket> findFirstByOrderByIdAsc();
}
