package com.myagree.app.rental;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    /** The owner's own UPI QR, shown to farmers who pay a booking by Scan & Pay. */
    @PostMapping(path = "/me/upi-qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    VehicleOwnerProfileResponse uploadUpiQr(CurrentUser user, @RequestPart("image") MultipartFile image, Language language) {
        return ownerService.uploadUpiQr(user.requireOwnerId(), image, language);
    }

    @DeleteMapping("/me/upi-qr")
    VehicleOwnerProfileResponse removeUpiQr(CurrentUser user, Language language) {
        return ownerService.removeUpiQr(user.requireOwnerId(), language);
    }

    @GetMapping("/dashboard")
    OwnerDashboardResponse dashboard(CurrentUser user, Language language) {
        return ownerService.dashboard(user.requireOwnerId(), language);
    }
}
