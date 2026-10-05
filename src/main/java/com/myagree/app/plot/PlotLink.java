package com.myagree.app.plot;

import org.jspecify.annotations.Nullable;

/**
 * What a new scan needs to know about the plot it was taken on.
 *
 * @param label                 e.g. "North Field • Plot A"
 * @param activeTreatmentPlanId the plan already running on the plot, if any
 */
public record PlotLink(long plotId, String label, @Nullable Long activeTreatmentPlanId) {
}
