package com.myagree.app.rental;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The farmer who asked for a booking, as the owner sees them on it. Kept with the booking as it was when they booked,
 * so the owner's list needs no other feature's data.
 *
 * @param id       the farmer profile; every farmer-side query is scoped by it
 * @param userId   the farmer's account, which notifications go to
 * @param name     e.g. "Rishikesh"
 * @param phone    the 10-digit mobile number the owner calls
 * @param location e.g. "Solapur, Maharashtra"
 */
@Embeddable
record BookingFarmer(
        @Column(name = "farmer_id", nullable = false, updatable = false) long id,
        @Column(name = "farmer_user_id", nullable = false, updatable = false) long userId,
        @Column(name = "farmer_name", nullable = false, updatable = false) String name,
        @Column(name = "farmer_phone", nullable = false, updatable = false, length = 10) String phone,
        @Column(name = "farmer_location", nullable = false, updatable = false) String location) {
}
