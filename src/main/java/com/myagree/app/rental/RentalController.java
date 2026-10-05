package com.myagree.app.rental;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.rental.dto.BookRentalRequest;
import com.myagree.app.rental.dto.RentalBookingResponse;
import com.myagree.app.rental.dto.RentalListingResponse;
import com.myagree.app.rental.dto.RentalsOverviewResponse;

@RestController
@RequestMapping("/api/rentals")
class RentalController {

    private final RentalService rentalService;

    RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @GetMapping
    RentalsOverviewResponse overview() {
        return rentalService.overview();
    }

    @PostMapping("/{id}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    RentalBookingResponse book(@PathVariable long id, @RequestBody(required = false) @Valid @Nullable BookRentalRequest request) {
        return rentalService.book(id, request != null ? request.slotLabel() : null);
    }

    @PostMapping("/{id}/favorite")
    RentalListingResponse toggleFavorite(@PathVariable long id) {
        return rentalService.toggleFavorite(id);
    }
}
