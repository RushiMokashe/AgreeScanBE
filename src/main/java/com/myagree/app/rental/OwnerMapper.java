package com.myagree.app.rental;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.LocalizedText;
import com.myagree.app.common.i18n.LocalizedTextDto;
import com.myagree.app.rental.dto.OwnerFeatureDto;
import com.myagree.app.rental.dto.OwnerListingRequest;
import com.myagree.app.rental.dto.OwnerListingResponse;
import com.myagree.app.rental.dto.OwnerSpecDto;
import com.myagree.app.rental.dto.VehicleOwnerProfileResponse;

/** Shows an owner their profile and vehicles, with every editable text in all three languages. */
@Component
class OwnerMapper {

    private final ListingPictures pictures;

    OwnerMapper(ListingPictures pictures) {
        this.pictures = pictures;
    }

    VehicleOwnerProfileResponse toResponse(VehicleOwner owner, Language language) {
        RentalHub hub = owner.getHub();
        return new VehicleOwnerProfileResponse(owner.getId(), owner.getUserId(), owner.getName(),
                owner.getBusinessName(), owner.getPhone(), hub.getId(), hub.getName().resolve(language),
                owner.getUpiId(), pictures.upiQrUrl(owner), owner.acceptsScanAndPay());
    }

    /** @param openBookings the vehicle's requested and accepted bookings */
    OwnerListingResponse toResponse(RentalListing listing, long openBookings) {
        return new OwnerListingResponse(
                listing.getId(),
                listing.getCategory(),
                listing.getHub().getId(),
                LocalizedTextDto.from(listing.getName()),
                LocalizedTextDto.from(listing.getDescription()),
                pictures.imageUrl(listing),
                listing.getIcon(),
                listing.getRate(),
                listing.getRateUnit(),
                toDto(listing.getRateNote()),
                LocalizedTextDto.from(listing.getAvailability()),
                listing.isOnline(),
                listing.getSpecs().stream()
                        .map(spec -> new OwnerSpecDto(LocalizedTextDto.from(spec.value()), LocalizedTextDto.from(spec.label())))
                        .toList(),
                listing.getFeatures().stream()
                        .map(feature -> new OwnerFeatureDto(feature.icon(), LocalizedTextDto.from(feature.text())))
                        .toList(),
                openBookings);
    }

    /** The owner's input as listing details, at {@code hub}, which the caller has looked up. */
    static ListingDetails toDetails(OwnerListingRequest request, RentalHub hub) {
        return new ListingDetails(
                request.category(),
                hub,
                request.name().toLocalizedText(),
                request.description().toLocalizedText(),
                request.icon(),
                request.rate(),
                request.rateUnit(),
                request.rateNote() != null ? request.rateNote().toLocalizedText() : null,
                request.availability().toLocalizedText(),
                request.online(),
                request.specs().stream()
                        .map(spec -> new RentalSpec(spec.value().toLocalizedText(), spec.label().toLocalizedText()))
                        .toList(),
                request.features().stream()
                        .map(feature -> new RentalFeature(feature.icon(), feature.text().toLocalizedText()))
                        .toList());
    }

    private static @Nullable LocalizedTextDto toDto(@Nullable LocalizedText text) {
        return text != null ? LocalizedTextDto.from(text) : null;
    }
}
