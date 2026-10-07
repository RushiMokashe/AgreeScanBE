package com.myagree.app.rental;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface RentalHubRepository extends JpaRepository<RentalHub, Long> {

    List<RentalHub> findAllByOrderByIdAsc();

    Optional<RentalHub> findFirstByOrderByIdAsc();
}
