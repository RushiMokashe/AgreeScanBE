package com.myagree.app.care;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AgronomistRepository extends JpaRepository<Agronomist, Long> {

    /** An on-duty agronomist if there is one, otherwise the first registered agronomist. */
    Optional<Agronomist> findFirstByOrderByOnDutyDescIdAsc();
}
