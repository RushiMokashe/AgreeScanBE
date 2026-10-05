package com.myagree.app.treatment;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.NotFoundException;
import com.myagree.app.treatment.dto.TreatmentPlanResponse;

@Service
@Transactional(readOnly = true)
public class TreatmentService {

    private final TreatmentPlanRepository planRepository;
    private final Clock clock;

    public TreatmentService(TreatmentPlanRepository planRepository, Clock clock) {
        this.planRepository = planRepository;
        this.clock = clock;
    }

    /** The plan the farmer should be working on: the newest one with steps left, otherwise the newest plan. */
    public TreatmentPlanResponse activePlan() {
        long planId = planRepository.findFirstByStepsCompletedAtIsNullOrderByStartDateDescIdDesc()
                .or(planRepository::findFirstByOrderByStartDateDescIdDesc)
                .map(TreatmentPlan::getId)
                .orElseThrow(() -> new NotFoundException("No treatment plan has been created yet"));
        return toResponse(loadPlan(planId));
    }

    /** One treatment plan with its timeline. */
    public TreatmentPlanResponse getPlan(long planId) {
        return toResponse(loadPlan(planId));
    }

    /** Marks a step done now and returns the updated plan; completing a finished step changes nothing. */
    @Transactional
    public TreatmentPlanResponse completeStep(long planId, long stepId) {
        TreatmentPlan plan = loadPlan(planId);
        findStep(plan, stepId).complete(clock.instant());
        return toResponse(plan);
    }

    /** Switches a step's alarm reminder on or off and returns the updated plan. */
    @Transactional
    public TreatmentPlanResponse toggleStepReminder(long planId, long stepId) {
        TreatmentPlan plan = loadPlan(planId);
        findStep(plan, stepId).toggleReminder();
        return toResponse(plan);
    }

    private TreatmentPlan loadPlan(long planId) {
        return planRepository.findWithStepsById(planId)
                .orElseThrow(() -> NotFoundException.of("Treatment plan", planId));
    }

    private static TreatmentStep findStep(TreatmentPlan plan, long stepId) {
        return plan.findStep(stepId).orElseThrow(() -> new NotFoundException(
                "Step %d not found in treatment plan %d".formatted(stepId, plan.getId())));
    }

    private TreatmentPlanResponse toResponse(TreatmentPlan plan) {
        return TreatmentMapper.toResponse(plan, LocalDate.now(clock));
    }
}
