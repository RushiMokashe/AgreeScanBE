package com.myagree.app.scan;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanRepository extends JpaRepository<Scan, Long> {

    List<Scan> findByOrderByScannedAtDescIdDesc(Limit limit);

    Optional<Scan> findFirstByOrderByScannedAtDescIdDesc();

    @EntityGraph(attributePaths = "sanitationSteps")
    Optional<Scan> findWithSanitationStepsById(long id);
}
