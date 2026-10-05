package com.myagree.app.rental;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalBookingRepository extends JpaRepository<RentalBooking, Long> {
}
