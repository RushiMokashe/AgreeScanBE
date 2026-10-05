package com.myagree.app.plot.dto;

import com.myagree.app.common.Tone;

/** Mirrors {@code PlotMetric} in frontend/src/lib/types.ts. */
public record PlotMetricResponse(String label, String value, Tone tone) {
}
