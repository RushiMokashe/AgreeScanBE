package com.myagree.app.rental;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalHubRepository extends JpaRepository<RentalHub, Long> {

    Optional<RentalHub> findFirstByOrderByIdAsc();
}
