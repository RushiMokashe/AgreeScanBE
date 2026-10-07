package com.myagree.app.rental;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface VehicleOwnerRepository extends JpaRepository<VehicleOwner, Long> {

    @EntityGraph(attributePaths = "hub")
    Optional<VehicleOwner> findWithHubById(long id);
}
