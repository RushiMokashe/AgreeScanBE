package com.myagree.app.common.spi;

import java.util.Map;

/**
 * Reports the numbers of the admin overview ({@code GET /api/admin/overview}) that one slice owns.
 *
 * <p><b>Implemented by</b> the rental slice ({@link AdminMetric#ACTIVE_LISTINGS}, {@link AdminMetric#PENDING_BOOKINGS}),
 * the store slice ({@link AdminMetric#ORDERS_TODAY}), the payment slice ({@link AdminMetric#REVENUE_THIS_MONTH}) and
 * the farm slice ({@link AdminMetric#SCANS_THIS_WEEK}). <b>Consumed by</b> the admin slice, which injects
 * {@code List<AdminMetricsContributor>}, merges the maps and shows 0 for a metric no slice reports yet.
 *
 * <p><b>Contract:</b> read-only; every metric is reported by exactly one slice (the admin slice refuses a metric
 * reported twice); values are never negative.
 */
public interface AdminMetricsContributor {

    /** The current value of each metric this slice owns. */
    Map<AdminMetric, Long> metrics();
}
