package com.myagree.app.rental;

/** How many bookings of some kind one listing has; see {@link RentalBookingRepository#countPerListing}. */
record ListingBookingCount(long listingId, long bookings) {
}
