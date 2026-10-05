package com.myagree.app.common.spi;

/**
 * A number on the admin overview ({@code AdminOverview} in frontend/src/lib/types.ts), with the slice that reports it
 * through {@link AdminMetricsContributor}. Days, weeks (from Monday) and months are reckoned in India time
 * ({@code ClockConfig.FARM_ZONE}) from the injected {@code Clock}.
 */
public enum AdminMetric {

    /** Rental listings their owners have switched online. Reported by the rental slice. */
    ACTIVE_LISTINGS,

    /** Bookings waiting for the owner's answer ({@code REQUESTED}). Reported by the rental slice. */
    PENDING_BOOKINGS,

    /** Store orders placed today. Reported by the store slice. */
    ORDERS_TODAY,

    /** Rupees received this month: the amounts of {@code SUCCEEDED} payments. Reported by the payment slice. */
    REVENUE_THIS_MONTH,

    /** Crop scans made this week. Reported by the farm slice. */
    SCANS_THIS_WEEK
}
