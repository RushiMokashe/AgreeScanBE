package com.myagree.app.rental.dto;

import java.util.List;

/**
 * Mirrors {@code OwnerDashboard} in frontend/src/lib/types.ts.
 *
 * @param earningsThisMonth rupees paid online for jobs completed this month (India time)
 * @param upcoming          accepted bookings, oldest request first, at most five
 */
public record OwnerDashboardResponse(
        long listingsTotal,
        long listingsOnline,
        long pendingRequests,
        long earningsThisMonth,
        List<OwnerBookingResponse> upcoming) {
}
