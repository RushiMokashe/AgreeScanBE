package com.myagree.app.rental;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.myagree.app.common.media.MediaContent;
import com.myagree.app.common.media.MediaSource;
import com.myagree.app.common.media.PhotoStore;

/** Serves owners' vehicle photos through signed media URLs: {@code /api/media/listing/{fileName}}. */
@Component
class ListingPhotoSource implements MediaSource {

    private final RentalListingRepository listingRepository;
    private final PhotoStore photoStore;

    ListingPhotoSource(RentalListingRepository listingRepository, PhotoStore photoStore) {
        this.listingRepository = listingRepository;
        this.photoStore = photoStore;
    }

    @Override
    public String kind() {
        return ListingPictures.MEDIA_KIND;
    }

    /** The photo a listing shows now; a photo replaced since is no longer served. */
    @Override
    public Optional<MediaContent> load(String id) {
        return listingRepository.findByPhotoFileName(id)
                .flatMap(listing -> photoStore.load(ListingPictures.MEDIA_KIND, listing.getPhoto()));
    }
}
