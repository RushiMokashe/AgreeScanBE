package com.myagree.app.rental;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.rental.dto.OwnerDashboardResponse;
import com.myagree.app.rental.dto.OwnerProfileRequest;
import com.myagree.app.rental.dto.VehicleOwnerProfileResponse;

/** The owner portal's profile and dashboard; {@code /api/owner/**} is open to the VEHICLE_OWNER role only. */
@RestController
@RequestMapping("/api/owner")
class OwnerProfileController {

    private final VehicleOwnerService ownerService;

    OwnerProfileController(VehicleOwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping("/me")
    VehicleOwnerProfileResponse profile(CurrentUser user, Language language) {
        return ownerService.profile(user.requireOwnerId(), language);
    }

    @PutMapping("/me")
    VehicleOwnerProfileResponse updateProfile(CurrentUser user, @Valid @RequestBody OwnerProfileRequest request,
                                              Language language) {
        return ownerService.updateProfile(user.requireOwnerId(), request, language);
    }

    @GetMapping("/dashboard")
    OwnerDashboardResponse dashboard(CurrentUser user, Language language) {
        return ownerService.dashboard(user.requireOwnerId(), language);
    }
}
