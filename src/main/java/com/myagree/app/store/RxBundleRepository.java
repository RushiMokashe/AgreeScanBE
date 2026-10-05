package com.myagree.app.store;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RxBundleRepository extends JpaRepository<RxBundle, Long> {

    @EntityGraph(attributePaths = "product")
    Optional<RxBundle> findFirstByOrderByPrescribedAtDesc();
}
