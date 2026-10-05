package com.myagree.app.plot.dto;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.myagree.app.plot.PlotHealth;

/** Mirrors {@code FieldPlot} in frontend/src/lib/types.ts. */
public record FieldPlotResponse(
        long id,
        String code,
        String fieldName,
        String crop,
        String variety,
        double areaAcres,
        String imageUrl,
        PlotHealth health,
        List<PlotMetricResponse> metrics,
        @Nullable String pendingAction,
        @Nullable Long activeTreatmentPlanId,
        @Nullable Long latestScanId) {
}
