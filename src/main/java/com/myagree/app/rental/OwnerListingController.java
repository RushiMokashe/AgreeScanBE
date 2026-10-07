package com.myagree.app.rental;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.rental.dto.ListingOnlineRequest;
import com.myagree.app.rental.dto.OwnerListingRequest;
import com.myagree.app.rental.dto.OwnerListingResponse;

/** The owner portal's vehicles: list, add, edit, switch online, photograph and remove. */
@RestController
@RequestMapping("/api/owner/listings")
class OwnerListingController {

    private final VehicleOwnerService ownerService;

    OwnerListingController(VehicleOwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping
    List<OwnerListingResponse> listings(CurrentUser user) {
        return ownerService.listings(user.requireOwnerId());
    }

    @GetMapping("/{id}")
    OwnerListingResponse listing(CurrentUser user, @PathVariable long id) {
        return ownerService.listing(user.requireOwnerId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OwnerListingResponse create(CurrentUser user, @Valid @RequestBody OwnerListingRequest request) {
        return ownerService.create(user.requireOwnerId(), request);
    }

    @PutMapping("/{id}")
    OwnerListingResponse update(CurrentUser user, @PathVariable long id, @Valid @RequestBody OwnerListingRequest request) {
        return ownerService.update(user.requireOwnerId(), id, request);
    }

    @PatchMapping("/{id}/online")
    OwnerListingResponse setOnline(CurrentUser user, @PathVariable long id,
                                   @Valid @RequestBody ListingOnlineRequest request) {
        return ownerService.setOnline(user.requireOwnerId(), id, request.online());
    }

    @PostMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    OwnerListingResponse uploadPhoto(CurrentUser user, @PathVariable long id, @RequestPart("image") MultipartFile image) {
        return ownerService.uploadPhoto(user.requireOwnerId(), id, image);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(CurrentUser user, @PathVariable long id) {
        ownerService.delete(user.requireOwnerId(), id);
    }
}
