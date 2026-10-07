package com.myagree.app.rental;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.NotFoundException;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.spi.FarmerPreferencesReader;
import com.myagree.app.rental.dto.AdminRentalListingResponse;
import com.myagree.app.rental.dto.AdminRentalUpdateRequest;
import com.myagree.app.rental.dto.RentalHubOptionResponse;
import com.myagree.app.rental.dto.RentalListingResponse;
import com.myagree.app.rental.dto.RentalsOverviewResponse;

/**
 * The vehicles farmers can hire: the hubs, each hub's vehicles taking bookings with its spotlight offer, and every
 * farmer's own favourites; plus the admin's view of all vehicles and their rates.
 */
@Service
@Transactional(readOnly = true)
public class RentalCatalogService {

    private static final String HUB_NOT_FOUND = "rental.hub.not-found";
    private static final String LISTING_NOT_FOUND = "rental.listing.not-found";

    private final RentalHubRepository hubRepository;
    private final RentalListingRepository listingRepository;
    private final RentalSpotlightRepository spotlightRepository;
    private final RentalFavoriteRepository favoriteRepository;
    private final ObjectProvider<FarmerPreferencesReader> preferences;
    private final RentalMapper mapper;

    RentalCatalogService(RentalHubRepository hubRepository, RentalListingRepository listingRepository,
                         RentalSpotlightRepository spotlightRepository, RentalFavoriteRepository favoriteRepository,
                         ObjectProvider<FarmerPreferencesReader> preferences, RentalMapper mapper) {
        this.hubRepository = hubRepository;
        this.listingRepository = listingRepository;
        this.spotlightRepository = spotlightRepository;
        this.favoriteRepository = favoriteRepository;
        this.preferences = preferences;
        this.mapper = mapper;
    }

    /** Every hub, for the rentals screen's hub switcher. */
    public List<RentalHubOptionResponse> hubs(Language language) {
        return hubRepository.findAllByOrderByIdAsc().stream().map(hub -> mapper.toOption(hub, language)).toList();
    }

    /**
     * A hub's vehicles taking bookings, with its spotlight offer and the farmer's favourites marked.
     *
     * @param hubId the hub to show; {@code null} for the farmer's preferred hub, else the first hub
     * @throws NotFoundException for an unknown hub, or when there is no hub at all
     */
    public RentalsOverviewResponse overview(long farmerId, @Nullable Long hubId, Language language) {
        RentalHub hub = hubId != null ? findHub(hubId) : defaultHub(farmerId);
        RentalSpotlight spotlight = spotlightRepository.findBookableInHub(hub.getId()).orElse(null);
        List<RentalListing> listings = listingRepository.findBookableInHub(hub.getId()).stream()
                .filter(listing -> spotlight == null || !listing.getId().equals(spotlight.getListing().getId()))
                .toList();
        Set<Long> favorites = favoriteRepository.findListingIdsByFarmerId(farmerId);
        return mapper.toOverview(hub, spotlight, listings, favorites, language);
    }

    /**
     * Adds the listing to, or removes it from, the farmer's saved rentals.
     *
     * @throws NotFoundException when there is no such listing
     */
    @Transactional
    public RentalListingResponse toggleFavorite(long farmerId, long listingId, Language language) {
        RentalListing listing = listingRepository.findWithDetailsByIdAndRemovedFalse(listingId)
                .orElseThrow(() -> listingNotFound(listingId));
        Optional<RentalFavorite> saved = favoriteRepository.findByFarmerIdAndListingId(farmerId, listingId);
        saved.ifPresentOrElse(favoriteRepository::delete,
                () -> favoriteRepository.save(new RentalFavorite(farmerId, listing)));
        return mapper.toResponse(listing, saved.isEmpty(), language);
    }

    /** Every vehicle on AgriScan with its owner, hub and rate, for the admin portal. */
    public List<AdminRentalListingResponse> adminListings(Language language) {
        return listingRepository.findByRemovedFalseOrderByIdAsc().stream()
                .map(listing -> mapper.toAdminResponse(listing, language))
                .toList();
    }

    /**
     * Corrects a vehicle's rate or takes it on or off line on the owner's behalf; fields left out stay as they are.
     *
     * @throws NotFoundException when there is no such listing
     */
    @Transactional
    public AdminRentalListingResponse adminUpdate(long listingId, AdminRentalUpdateRequest update, Language language) {
        RentalListing listing = listingRepository.findWithOwnerAndHubByIdAndRemovedFalse(listingId)
                .orElseThrow(() -> listingNotFound(listingId));
        if (update.rate() != null || update.rateUnit() != null) {
            listing.reprice(update.rate() != null ? update.rate() : listing.getRate(),
                    update.rateUnit() != null ? update.rateUnit() : listing.getRateUnit());
        }
        if (update.online() != null) {
            listing.changeOnline(update.online());
        }
        return mapper.toAdminResponse(listing, language);
    }

    /** The farmer's preferred hub while it exists, else the first hub; empty only when there is no hub at all. */
    Optional<RentalHub> homeHub(long farmerId) {
        return Optional.ofNullable(preferences.getIfAvailable())
                .flatMap(reader -> reader.preferredRentalHub(farmerId))
                .flatMap(hubRepository::findById)
                .or(hubRepository::findFirstByOrderByIdAsc);
    }

    /** The vehicles taking bookings at a hub, its spotlight's included, in catalogue order. */
    List<RentalListing> bookableListings(RentalHub hub) {
        return listingRepository.findBookableInHub(hub.getId());
    }

    private RentalHub defaultHub(long farmerId) {
        return homeHub(farmerId).orElseThrow(() -> new NotFoundException("No rental hub serves the farmer's area"));
    }

    private RentalHub findHub(long hubId) {
        return hubRepository.findById(hubId)
                .orElseThrow(() -> new NotFoundException(UserMessage.of(HUB_NOT_FOUND, String.valueOf(hubId))));
    }

    static NotFoundException listingNotFound(long listingId) {
        return new NotFoundException(UserMessage.of(LISTING_NOT_FOUND, String.valueOf(listingId)));
    }
}
