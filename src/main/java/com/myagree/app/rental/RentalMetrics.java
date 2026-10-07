package com.myagree.app.rental;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.spi.AdminMetric;
import com.myagree.app.common.spi.AdminMetricsContributor;

/** The rental numbers of the admin overview: vehicles taking bookings and requests waiting for their owner. */
@Component
class RentalMetrics implements AdminMetricsContributor {

    private final RentalListingRepository listingRepository;
    private final RentalBookingRepository bookingRepository;

    RentalMetrics(RentalListingRepository listingRepository, RentalBookingRepository bookingRepository) {
        this.listingRepository = listingRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<AdminMetric, Long> metrics() {
        return Map.of(
                AdminMetric.ACTIVE_LISTINGS, listingRepository.countByOnlineTrueAndRemovedFalse(),
                AdminMetric.PENDING_BOOKINGS, bookingRepository.countByStatus(BookingStatus.REQUESTED));
    }
}
