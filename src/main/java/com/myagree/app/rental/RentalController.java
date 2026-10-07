package com.myagree.app.rental;

import java.util.List;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.rental.dto.BookRentalRequest;
import com.myagree.app.rental.dto.BookingReasonRequest;
import com.myagree.app.rental.dto.RentalBookingDetailResponse;
import com.myagree.app.rental.dto.RentalBookingResponse;
import com.myagree.app.rental.dto.RentalHubOptionResponse;
import com.myagree.app.rental.dto.RentalListingResponse;
import com.myagree.app.rental.dto.RentalsOverviewResponse;

/** The farmer's side of rentals: browsing hubs, booking vehicles and following the bookings. */
@RestController
@RequestMapping("/api/rentals")
class RentalController {

    private static final BookRentalRequest DEFAULT_BOOKING = new BookRentalRequest(null, null);

    private final RentalCatalogService catalogService;
    private final RentalBookingService bookingService;

    RentalController(RentalCatalogService catalogService, RentalBookingService bookingService) {
        this.catalogService = catalogService;
        this.bookingService = bookingService;
    }

    @GetMapping("/hubs")
    List<RentalHubOptionResponse> hubs(Language language) {
        return catalogService.hubs(language);
    }

    @GetMapping
    RentalsOverviewResponse overview(CurrentUser user, @RequestParam(required = false) @Nullable Long hubId,
                                     Language language) {
        return catalogService.overview(user.requireFarmerId(), hubId, language);
    }

    @PostMapping("/{id}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    RentalBookingResponse book(CurrentUser user, @PathVariable long id,
                               @RequestBody(required = false) @Valid @Nullable BookRentalRequest request,
                               Language language) {
        return bookingService.book(user, id, request != null ? request : DEFAULT_BOOKING, language);
    }

    @PostMapping("/{id}/favorite")
    RentalListingResponse toggleFavorite(CurrentUser user, @PathVariable long id, Language language) {
        return catalogService.toggleFavorite(user.requireFarmerId(), id, language);
    }

    @GetMapping("/bookings")
    List<RentalBookingResponse> bookings(CurrentUser user, Language language) {
        return bookingService.farmerBookings(user.requireFarmerId(), language);
    }

    @GetMapping("/bookings/{id}")
    RentalBookingDetailResponse booking(CurrentUser user, @PathVariable long id, Language language) {
        return bookingService.farmerBooking(user.requireFarmerId(), id, language);
    }

    @PostMapping("/bookings/{id}/cancel")
    RentalBookingDetailResponse cancel(CurrentUser user, @PathVariable long id,
                                       @RequestBody(required = false) @Valid @Nullable BookingReasonRequest request,
                                       Language language) {
        return bookingService.cancel(user.requireFarmerId(), id, request != null ? request.reason() : null, language);
    }
}
