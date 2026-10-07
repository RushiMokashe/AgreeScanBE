package com.myagree.app.rental;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.myagree.app.common.media.MediaContent;
import com.myagree.app.common.media.MediaSource;
import com.myagree.app.common.media.PhotoStore;

/** Serves the UPI QR codes vehicle owners uploaded, shown to farmers who pay by Scan & Pay: {@code /api/media/owner-qr/{fileName}}. */
@Component
class OwnerQrSource implements MediaSource {

    private final VehicleOwnerRepository ownerRepository;
    private final PhotoStore photoStore;

    OwnerQrSource(VehicleOwnerRepository ownerRepository, PhotoStore photoStore) {
        this.ownerRepository = ownerRepository;
        this.photoStore = photoStore;
    }

    @Override
    public String kind() {
        return ListingPictures.OWNER_QR_KIND;
    }

    /** The QR an owner shows now; a QR replaced or removed since is no longer served. */
    @Override
    public Optional<MediaContent> load(String id) {
        return ownerRepository.findByUpiQrFileName(id)
                .flatMap(owner -> photoStore.load(ListingPictures.OWNER_QR_KIND, Objects.requireNonNull(owner.getUpiQr())));
    }
}
