package com.myagree.app.treatment.dto;

import java.time.LocalDate;
import java.util.List;

/** Mirrors {@code TreatmentPlan} in frontend/src/lib/types.ts. */
public record TreatmentPlanResponse(
        long id,
        long plotId,
        String plotCode,
        String title,
        String target,
        String pathogen,
        long scanId,
        LocalDate startDate,
        int durationDays,
        int completedSteps,
        int totalSteps,
        int progressPercent,
        List<TreatmentStepResponse> steps) {
}
