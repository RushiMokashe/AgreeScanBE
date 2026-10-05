package com.myagree.app.seed;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

import com.myagree.app.plot.Plot;
import com.myagree.app.scan.Scan;
import com.myagree.app.treatment.TreatmentPlan;
import com.myagree.app.treatment.TreatmentPlanRepository;

/**
 * The 7-day Early Blight protocol from the treatment tracker, started today with the first spray done. It is
 * created from the Early Blight scan, then both that scan and the tomato plot point at it.
 */
@Component
class TreatmentSeed implements DemoSeed {

    private static final int PROTOCOL_DAYS = 7;
    /** "Completed at 07:30 AM" in the design. */
    private static final LocalTime FIRST_SPRAY_TIME = LocalTime.of(7, 30);
    private static final String MORNING_REMINDER = "07:00 AM";

    private final TreatmentPlanRepository planRepository;
    private final Clock clock;

    TreatmentSeed(TreatmentPlanRepository planRepository, Clock clock) {
        this.planRepository = planRepository;
        this.clock = clock;
    }

    @Override
    public int order() {
        return 60;
    }

    @Override
    public void seed(SeedContext context) {
        Plot plot = context.get(PlotSeed.PLOTS).tomato();
        Scan diagnosis = context.get(ScanSeed.SCANS).earlyBlight();
        TreatmentPlan plan = createPlan(plot, diagnosis);
        plot.assignTreatmentPlan(plan.getId());
        diagnosis.assignTreatmentPlan(plan.getId());
    }

    private TreatmentPlan createPlan(Plot plot, Scan diagnosis) {
        LocalDate today = LocalDate.now(clock);
        TreatmentPlan plan = new TreatmentPlan(plot.getId(), plot.getCode(), "Treatment Plan: " + plot.getCode(),
                diagnosis.getCropLabel() + " " + diagnosis.getDiseaseName(), diagnosis.getPathogen(),
                diagnosis.getId(), today, PROTOCOL_DAYS);
        plan.addStep(1, "Fungicide Spray 1", "Mancozeb 75% WP • **40g per 15L Knapsack Pump**",
                        "Applied with fine hollow-cone nozzle under dry leaf surface", null, null)
                .complete(firstSprayCompletedAt(today));
        plan.addStep(4, "Field Check & Foliage Pruning",
                "Inspect lower canopy. Safely bag & burn residual wilted leaves showing concentric yellow rings.",
                null, "Critical Check", MORNING_REMINDER);
        plan.addStep(7, "Fungicide Spray 2 (Rotate Mode of Action)",
                "Azoxystrobin 23% SC • **1 ml / Litre of clean water**",
                "Rotating chemistry prevents fungal resistance buildup in %s.".formatted(plot.getCode()),
                "FRAC Group Rotation", MORNING_REMINDER);
        return planRepository.save(plan);
    }

    /** 07:30 today, or now when the app starts earlier than that: a step cannot be completed in the future. */
    private Instant firstSprayCompletedAt(LocalDate today) {
        Instant sprayTime = today.atTime(FIRST_SPRAY_TIME).atZone(clock.getZone()).toInstant();
        Instant now = clock.instant();
        return sprayTime.isBefore(now) ? sprayTime : now;
    }
}
