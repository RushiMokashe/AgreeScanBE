package com.myagree.app.rental;

/**
 * Who moved a booking on; mirrors the {@code actor} of {@code BookingEvent} in frontend/src/lib/types.ts. The lifecycle
 * of D9 is driven by farmers and owners; {@link #SYSTEM} is part of the contract for changes AgriScan makes by itself.
 */
public enum BookingActor {
    FARMER,
    OWNER,
    SYSTEM
}
