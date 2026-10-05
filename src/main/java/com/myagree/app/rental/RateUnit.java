package com.myagree.app.rental;

/**
 * What a rental rate is charged per; mirrors {@code RateUnit} in frontend/src/lib/types.ts. A booking estimates how many
 * units the job takes, {@link #defaultUnits()} when the farmer does not say.
 */
public enum RateUnit {
    HOUR(2),
    KM(15),
    DAY(1);

    private final int defaultUnits;

    RateUnit(int defaultUnits) {
        this.defaultUnits = defaultUnits;
    }

    /** The estimate of a booking that names none: 2 hours, 15 km or 1 day. */
    public int defaultUnits() {
        return defaultUnits;
    }
}
