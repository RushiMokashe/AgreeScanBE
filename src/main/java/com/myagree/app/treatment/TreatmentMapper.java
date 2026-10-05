package com.myagree.app.treatment;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.myagree.app.treatment.dto.TreatmentPlanResponse;
import com.myagree.app.treatment.dto.TreatmentStepResponse;

final class TreatmentMapper {

    private static final String TODAY = "Today";
    private static final String YESTERDAY = "Yesterday";
    private static final String SCHEDULED = "Scheduled";
    private static final int FULL_PERCENT = 100;

    private TreatmentMapper() {
    }

    static TreatmentPlanResponse toResponse(TreatmentPlan plan, LocalDate today) {
        List<TreatmentStepResponse> steps = plan.getSteps().stream()
                .map(step -> toResponse(plan, step, today))
                .toList();
        int completedSteps = plan.completedStepCount();
        return new TreatmentPlanResponse(
                plan.getId(),
                plan.getPlotId(),
                plan.getPlotCode(),
                plan.getTitle(),
                plan.getTarget(),
                plan.getPathogen(),
                plan.getScanId(),
                plan.getStartDate(),
                plan.getDurationDays(),
                completedSteps,
                steps.size(),
                progressPercent(completedSteps, steps.size()),
                steps);
    }

    /**
     * Relative label of a step's date: "Today", "In 3 Days" for the next step, "Scheduled" for later ones,
     * "2 Days Overdue" for missed ones and "Yesterday" / "3 Days Ago" for finished ones.
     */
    static String dayLabel(StepStatus status, LocalDate scheduledDate, LocalDate today) {
        long daysFromToday = ChronoUnit.DAYS.between(today, scheduledDate);
        if (daysFromToday == 0) {
            return TODAY;
        }
        if (daysFromToday < 0) {
            long daysAgo = -daysFromToday;
            if (status != StepStatus.COMPLETED) {
                return days(daysAgo) + " Overdue";
            }
            return daysAgo == 1 ? YESTERDAY : days(daysAgo) + " Ago";
        }
        return status == StepStatus.SCHEDULED ? SCHEDULED : "In " + days(daysFromToday);
    }

    private static TreatmentStepResponse toResponse(TreatmentPlan plan, TreatmentStep step, LocalDate today) {
        StepStatus status = plan.statusOf(step);
        LocalDate scheduledDate = plan.scheduledDateOf(step);
        return new TreatmentStepResponse(
                step.getId(),
                step.getDayNumber(),
                dayLabel(status, scheduledDate, today),
                scheduledDate,
                step.getTitle(),
                step.getDetail(),
                step.getNote(),
                step.getBadge(),
                status,
                step.getCompletedAt(),
                step.getReminderTime(),
                step.isReminderSet());
    }

    private static int progressPercent(int completedSteps, int totalSteps) {
        return totalSteps == 0 ? 0 : Math.round(completedSteps * (float) FULL_PERCENT / totalSteps);
    }

    private static String days(long count) {
        return count == 1 ? "1 Day" : count + " Days";
    }
}
