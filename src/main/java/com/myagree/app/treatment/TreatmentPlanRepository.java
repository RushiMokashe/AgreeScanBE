package com.myagree.app.treatment;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, Long> {

    @EntityGraph(attributePaths = "steps")
    Optional<TreatmentPlan> findWithStepsById(long id);

    /** The newest plan that still has at least one step to do. */
    Optional<TreatmentPlan> findFirstByStepsCompletedAtIsNullOrderByStartDateDescIdDesc();

    Optional<TreatmentPlan> findFirstByOrderByStartDateDescIdDesc();
}
