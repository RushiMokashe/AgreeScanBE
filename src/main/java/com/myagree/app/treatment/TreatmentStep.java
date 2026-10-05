package com.myagree.app.treatment;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.jspecify.annotations.Nullable;

/** One dated action in a treatment plan, e.g. "Day 4: Field Check & Foliage Pruning". */
@Entity
public class TreatmentStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id")
    private TreatmentPlan plan;

    private int dayNumber;
    private String title;
    private String detail;
    private @Nullable String note;
    private @Nullable String badge;
    private @Nullable String reminderTime;
    private boolean reminderSet;
    private @Nullable Instant completedAt;

    protected TreatmentStep() {
    }

    TreatmentStep(TreatmentPlan plan, int dayNumber, String title, String detail,
                  @Nullable String note, @Nullable String badge, @Nullable String reminderTime) {
        this.plan = plan;
        this.dayNumber = dayNumber;
        this.title = title;
        this.detail = detail;
        this.note = note;
        this.badge = badge;
        this.reminderTime = reminderTime;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    /** Marks the step done. Completing it again keeps the original completion time. */
    public void complete(Instant at) {
        if (completedAt == null) {
            completedAt = at;
        }
    }

    public void toggleReminder() {
        reminderSet = !reminderSet;
    }

    public Long getId() {
        return id;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public @Nullable String getNote() {
        return note;
    }

    public @Nullable String getBadge() {
        return badge;
    }

    public @Nullable String getReminderTime() {
        return reminderTime;
    }

    public boolean isReminderSet() {
        return reminderSet;
    }

    public @Nullable Instant getCompletedAt() {
        return completedAt;
    }
}
