package com.myagree.app.rental;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.rental.dto.AdminRentalListingResponse;
import com.myagree.app.rental.dto.AdminRentalUpdateRequest;

/** The admin portal's rental rates; {@code /api/admin/**} is open to the ADMIN role only. */
@RestController
@RequestMapping("/api/admin/rentals")
class AdminRentalController {

    private final RentalCatalogService catalogService;

    AdminRentalController(RentalCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    List<AdminRentalListingResponse> listings(Language language) {
        return catalogService.adminListings(language);
    }

    @PatchMapping("/{id}")
    AdminRentalListingResponse update(@PathVariable long id, @Valid @RequestBody AdminRentalUpdateRequest update,
                                      Language language) {
        return catalogService.adminUpdate(id, update, language);
    }
}
