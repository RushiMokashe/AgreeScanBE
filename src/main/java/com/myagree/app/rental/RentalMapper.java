package com.myagree.app.rental;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.LocalizedText;
import com.myagree.app.common.i18n.Messages;
import com.myagree.app.rental.dto.AdminRentalListingResponse;
import com.myagree.app.rental.dto.RentalFeatureResponse;
import com.myagree.app.rental.dto.RentalHubOptionResponse;
import com.myagree.app.rental.dto.RentalListingResponse;
import com.myagree.app.rental.dto.RentalOperatorResponse;
import com.myagree.app.rental.dto.RentalPerkResponse;
import com.myagree.app.rental.dto.RentalSpecResponse;
import com.myagree.app.rental.dto.RentalSpotlightResponse;
import com.myagree.app.rental.dto.RentalsOverviewResponse;

/**
 * Shows hubs and listings to farmers and admins in the reader's language. A card AgriScan has not curated falls back
 * to defaults: the call button names the owner, the book button follows the category, and a new owner's track record
 * reads "New on AgriScan".
 */
@Component
class RentalMapper {

    private static final String CALL_LABEL = "rental.listing.call";
    private static final String BOOK_LABEL_PREFIX = "rental.listing.book.";
    private static final String NEW_OPERATOR = "rental.listing.new-operator";

    private final Messages messages;
    private final ListingPictures pictures;

    RentalMapper(Messages messages, ListingPictures pictures) {
        this.messages = messages;
        this.pictures = pictures;
    }

    RentalHubOptionResponse toOption(RentalHub hub, Language language) {
        return new RentalHubOptionResponse(hub.getId(), hub.getName().resolve(language), hub.getRadiusKm(),
                hub.getRoutes().resolve(language));
    }

    /**
     * @param spotlight the hub's spotlight offer, whose listing is left out of {@code listings} by the caller
     * @param favorites the ids of the listings the farmer saved
     */
    RentalsOverviewResponse toOverview(RentalHub hub, @Nullable RentalSpotlight spotlight, List<RentalListing> listings,
                                       Set<Long> favorites, Language language) {
        long onlineCount = listings.size() + (spotlight != null ? 1 : 0);
        return new RentalsOverviewResponse(
                hub.getId(),
                hub.getName().resolve(language),
                hub.getRadiusKm(),
                hub.getRoutes().resolve(language),
                onlineCount,
                spotlight != null ? toResponse(spotlight, language) : null,
                listings.stream().map(listing -> toResponse(listing, favorites.contains(listing.getId()), language))
                        .toList());
    }

    RentalListingResponse toResponse(RentalListing listing, boolean favorite, Language language) {
        VehicleOwner owner = listing.getOwner();
        return new RentalListingResponse(
                listing.getId(),
                listing.getCategory(),
                listing.getName().resolve(language),
                listing.getDescription().resolve(language),
                pictures.imageUrl(listing),
                pictures.icon(listing),
                resolve(listing.getBadge(), language),
                listing.getBadgeIcon(),
                listing.getDistanceKm(),
                resolve(listing.getLocality(), language),
                listing.getRate(),
                listing.getRateUnit(),
                resolve(listing.getRateNote(), language),
                listing.isRateNoteHighlighted(),
                new RentalOperatorResponse(owner.getName(), owner.initials(), operatorStats(listing, language)),
                listing.getAvailability().resolve(language),
                listing.isAvailabilityHighlighted(),
                listing.getSpecs().stream()
                        .map(spec -> new RentalSpecResponse(spec.value().resolve(language), spec.label().resolve(language)))
                        .toList(),
                listing.getFeatures().stream()
                        .map(feature -> new RentalFeatureResponse(feature.icon(), feature.text().resolve(language)))
                        .toList(),
                owner.dialablePhone(),
                callLabel(listing, language),
                bookLabel(listing, language),
                Objects.requireNonNullElse(listing.getBookIcon(), listing.getCategory().defaultBookIcon()),
                favorite);
    }

    AdminRentalListingResponse toAdminResponse(RentalListing listing, Language language) {
        VehicleOwner owner = listing.getOwner();
        return new AdminRentalListingResponse(
                listing.getId(),
                listing.getName().resolve(language),
                listing.getCategory(),
                listing.getHub().getName().resolve(language),
                owner.getName(),
                owner.getPhone(),
                listing.getRate(),
                listing.getRateUnit(),
                listing.isOnline(),
                pictures.imageUrl(listing),
                pictures.icon(listing));
    }

    private RentalSpotlightResponse toResponse(RentalSpotlight spotlight, Language language) {
        RentalListing listing = spotlight.getListing();
        return new RentalSpotlightResponse(
                listing.getId(),
                listing.getName().resolve(language),
                listing.getDescription().resolve(language),
                spotlight.getLabel().resolve(language),
                spotlight.getImageUrl(),
                Objects.requireNonNullElse(listing.getBaseFare(), 0),
                listing.getRate(),
                spotlight.getDispatchMinutes(),
                spotlight.getPerks().stream()
                        .map(perk -> new RentalPerkResponse(
                                perk.icon(), perk.title().resolve(language), perk.subtitle().resolve(language)))
                        .toList());
    }

    private String operatorStats(RentalListing listing, Language language) {
        LocalizedText stats = listing.getOperatorStats();
        return stats != null ? stats.resolve(language) : messages.get(NEW_OPERATOR, language);
    }

    private String callLabel(RentalListing listing, Language language) {
        LocalizedText label = listing.getCallLabel();
        return label != null ? label.resolve(language) : messages.get(CALL_LABEL, language, listing.getOwner().firstName());
    }

    private String bookLabel(RentalListing listing, Language language) {
        LocalizedText label = listing.getBookLabel();
        return label != null ? label.resolve(language) : messages.get(BOOK_LABEL_PREFIX + listing.getCategory().name(), language);
    }

    private static @Nullable String resolve(@Nullable LocalizedText text, Language language) {
        return text != null ? text.resolve(language) : null;
    }
}
