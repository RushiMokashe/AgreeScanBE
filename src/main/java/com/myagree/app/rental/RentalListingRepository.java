package com.myagree.app.rental;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalListingRepository extends JpaRepository<RentalListing, Long> {

    /** All listings except {@code excludedId} (the spotlight's own listing), in catalogue order. */
    @EntityGraph(attributePaths = {"specs", "features"})
    List<RentalListing> findByIdNotOrderByIdAsc(long excludedId);

    @EntityGraph(attributePaths = {"specs", "features"})
    Optional<RentalListing> findWithDetailsById(long id);
}
