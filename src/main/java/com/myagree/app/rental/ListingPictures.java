package com.myagree.app.rental;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.myagree.app.common.media.MediaUrlSigner;
import com.myagree.app.common.media.StoredPhoto;

/**
 * What a listing's card shows as its picture: the owner's uploaded photo through a signed media URL
 * (docs/architecture/phase-2.md, D7), else the photo shipped with the app, else an icon tile.
 */
@Component
class ListingPictures {

    /** The media kind of uploaded vehicle photos: {@code /api/media/listing/{fileName}}. */
    static final String MEDIA_KIND = "listing";

    private final MediaUrlSigner mediaUrlSigner;

    ListingPictures(MediaUrlSigner mediaUrlSigner) {
        this.mediaUrlSigner = mediaUrlSigner;
    }

    /** The photo's URL, or {@code null} for a card that shows an icon tile. */
    @Nullable String imageUrl(RentalListing listing) {
        StoredPhoto photo = listing.getPhoto();
        return photo != null ? mediaUrlSigner.sign(MEDIA_KIND, photo.fileName()) : listing.getImageUrl();
    }

    /** The icon tile of a card without a photo: the owner's choice, else the category's; as stored for a photo card. */
    @Nullable String icon(RentalListing listing) {
        if (listing.getIcon() != null || imageUrl(listing) != null) {
            return listing.getIcon();
        }
        return listing.getCategory().defaultIcon();
    }
}
