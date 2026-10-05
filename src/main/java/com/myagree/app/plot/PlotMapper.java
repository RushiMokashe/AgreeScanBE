package com.myagree.app.plot;

import com.myagree.app.plot.dto.FieldPlotResponse;
import com.myagree.app.plot.dto.PlotMetricResponse;

final class PlotMapper {

    private PlotMapper() {
    }

    static FieldPlotResponse toResponse(Plot plot) {
        return new FieldPlotResponse(
                plot.getId(),
                plot.getCode(),
                plot.getFieldName(),
                plot.getCrop().label(),
                plot.getVariety(),
                plot.getAreaAcres(),
                plot.getImageUrl(),
                plot.getHealth(),
                plot.getMetrics().stream().map(PlotMapper::toResponse).toList(),
                plot.getPendingAction(),
                plot.getActiveTreatmentPlanId(),
                plot.getLatestScanId());
    }

    static PlotLink toLink(Plot plot) {
        return new PlotLink(plot.getId(), plot.label(), plot.getActiveTreatmentPlanId());
    }

    private static PlotMetricResponse toResponse(PlotMetric metric) {
        return new PlotMetricResponse(metric.label(), metric.value(), metric.tone());
    }
}
