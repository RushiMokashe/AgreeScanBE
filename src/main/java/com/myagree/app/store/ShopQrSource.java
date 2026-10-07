package com.myagree.app.store;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.myagree.app.common.media.MediaContent;
import com.myagree.app.common.media.MediaSource;
import com.myagree.app.common.media.PhotoStore;

/** Serves the UPI QR codes shops uploaded, shown to farmers who pay by Scan & Pay: {@code /api/media/shop-qr/{fileName}}. */
@Component
class ShopQrSource implements MediaSource {

    private final ShopRepository shopRepository;
    private final PhotoStore photoStore;

    ShopQrSource(ShopRepository shopRepository, PhotoStore photoStore) {
        this.shopRepository = shopRepository;
        this.photoStore = photoStore;
    }

    @Override
    public String kind() {
        return StorePictures.SHOP_QR_KIND;
    }

    /** The QR a shop shows now; a QR replaced or removed since is no longer served. */
    @Override
    public Optional<MediaContent> load(String id) {
        return shopRepository.findByUpiQrFileName(id)
                .flatMap(shop -> photoStore.load(StorePictures.SHOP_QR_KIND, Objects.requireNonNull(shop.getUpiQr())));
    }
}
