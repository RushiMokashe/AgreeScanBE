package com.myagree.app.store;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreDepotRepository extends JpaRepository<StoreDepot, Long> {

    Optional<StoreDepot> findFirstByOrderByIdAsc();
}
