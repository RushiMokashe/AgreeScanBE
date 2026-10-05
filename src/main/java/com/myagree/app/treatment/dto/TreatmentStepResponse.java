package com.myagree.app.treatment.dto;

import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import com.myagree.app.treatment.StepStatus;

/** Mirrors {@code TreatmentStep} in frontend/src/lib/types.ts. */
public record TreatmentStepResponse(
        long id,
        int dayNumber,
        String dayLabel,
        LocalDate scheduledDate,
        String title,
        String detail,
        @Nullable String note,
        @Nullable String badge,
        StepStatus status,
        @Nullable Instant completedAt,
        @Nullable String reminderTime,
        boolean reminderSet) {
}
