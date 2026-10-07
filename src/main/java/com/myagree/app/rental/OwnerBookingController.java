package com.myagree.app.rental;

import java.util.List;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.rental.dto.BookingReasonRequest;
import com.myagree.app.rental.dto.OwnerBookingDetailResponse;
import com.myagree.app.rental.dto.OwnerBookingResponse;

/** The owner portal's bookings and the owner's moves of the lifecycle (docs/architecture/phase-2.md, D9). */
@RestController
@RequestMapping("/api/owner/bookings")
class OwnerBookingController {

    private final RentalBookingService bookingService;

    OwnerBookingController(RentalBookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    List<OwnerBookingResponse> bookings(CurrentUser user, @RequestParam(required = false) @Nullable BookingStatus status,
                                        Language language) {
        return bookingService.ownerBookings(user.requireOwnerId(), status, language);
    }

    @GetMapping("/{id}")
    OwnerBookingDetailResponse booking(CurrentUser user, @PathVariable long id, Language language) {
        return bookingService.ownerBooking(user.requireOwnerId(), id, language);
    }

    @PostMapping("/{id}/accept")
    OwnerBookingDetailResponse accept(CurrentUser user, @PathVariable long id, Language language) {
        return bookingService.accept(user.requireOwnerId(), id, language);
    }

    @PostMapping("/{id}/decline")
    OwnerBookingDetailResponse decline(CurrentUser user, @PathVariable long id,
                                       @RequestBody(required = false) @Valid @Nullable BookingReasonRequest request,
                                       Language language) {
        return bookingService.decline(user.requireOwnerId(), id, request != null ? request.reason() : null, language);
    }

    @PostMapping("/{id}/start")
    OwnerBookingDetailResponse start(CurrentUser user, @PathVariable long id, Language language) {
        return bookingService.start(user.requireOwnerId(), id, language);
    }

    @PostMapping("/{id}/complete")
    OwnerBookingDetailResponse complete(CurrentUser user, @PathVariable long id, Language language) {
        return bookingService.complete(user.requireOwnerId(), id, language);
    }
}
