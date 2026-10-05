package com.myagree.app.rental;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalSpotlightRepository extends JpaRepository<RentalSpotlight, Long> {

    @EntityGraph(attributePaths = "listing")
    Optional<RentalSpotlight> findFirstByOrderByIdAsc();
}
