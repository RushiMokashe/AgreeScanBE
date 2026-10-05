package com.myagree.app.treatment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import org.jspecify.annotations.Nullable;

/**
 * A day-by-day protocol against one diagnosed disease on one plot. The plot and the originating scan are
 * referenced by id; {@code plotCode} is kept as a snapshot for display.
 */
@Entity
public class TreatmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long plotId;
    private String plotCode;
    private String title;
    private String target;
    private String pathogen;
    private long scanId;
    private LocalDate startDate;
    private int durationDays;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber")
    private List<TreatmentStep> steps = new ArrayList<>();

    protected TreatmentPlan() {
    }

    public TreatmentPlan(long plotId, String plotCode, String title, String target, String pathogen,
                         long scanId, LocalDate startDate, int durationDays) {
        this.plotId = plotId;
        this.plotCode = plotCode;
        this.title = title;
        this.target = target;
        this.pathogen = pathogen;
        this.scanId = scanId;
        this.startDate = startDate;
        this.durationDays = durationDays;
    }

    /** Appends the step for {@code dayNumber}; steps must be added in day order. */
    public TreatmentStep addStep(int dayNumber, String title, String detail,
                                 @Nullable String note, @Nullable String badge, @Nullable String reminderTime) {
        TreatmentStep step = new TreatmentStep(this, dayNumber, title, detail, note, badge, reminderTime);
        steps.add(step);
        return step;
    }

    public Optional<TreatmentStep> findStep(long stepId) {
        return steps.stream()
                .filter(step -> Objects.equals(step.getId(), stepId))
                .findFirst();
    }

    /** Day 1 is the start date itself. */
    public LocalDate scheduledDateOf(TreatmentStep step) {
        return startDate.plusDays(step.getDayNumber() - 1L);
    }

    public StepStatus statusOf(TreatmentStep step) {
        if (step.isCompleted()) {
            return StepStatus.COMPLETED;
        }
        boolean isNextOpenStep = nextOpenStep().filter(next -> next == step).isPresent();
        return isNextOpenStep ? StepStatus.UPCOMING : StepStatus.SCHEDULED;
    }

    public int completedStepCount() {
        return (int) steps.stream().filter(TreatmentStep::isCompleted).count();
    }

    private Optional<TreatmentStep> nextOpenStep() {
        return steps.stream().filter(step -> !step.isCompleted()).findFirst();
    }

    public Long getId() {
        return id;
    }

    public long getPlotId() {
        return plotId;
    }

    public String getPlotCode() {
        return plotCode;
    }

    public String getTitle() {
        return title;
    }

    public String getTarget() {
        return target;
    }

    public String getPathogen() {
        return pathogen;
    }

    public long getScanId() {
        return scanId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public List<TreatmentStep> getSteps() {
        return List.copyOf(steps);
    }
}
