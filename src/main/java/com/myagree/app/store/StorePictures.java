package com.myagree.app.store;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.myagree.app.common.media.MediaUrlSigner;
import com.myagree.app.common.media.StoredPhoto;

/** The URLs of the store's uploaded pictures: product photos and shops' UPI QR codes, through signed media URLs. */
@Component
class StorePictures {

    /** Shopkeepers' product photos: {@code /api/media/product/{fileName}}. */
    static final String PRODUCT_KIND = "product";
    /** Shops' uploaded UPI QR codes: {@code /api/media/shop-qr/{fileName}}. */
    static final String SHOP_QR_KIND = "shop-qr";

    private final MediaUrlSigner mediaUrlSigner;

    StorePictures(MediaUrlSigner mediaUrlSigner) {
        this.mediaUrlSigner = mediaUrlSigner;
    }

    /** The uploaded photo, else the one shipped with the app; {@code null} for a card that shows a category tile. */
    @Nullable String imageUrl(Product product) {
        StoredPhoto photo = product.getPhoto();
        return photo != null ? mediaUrlSigner.sign(PRODUCT_KIND, photo.fileName()) : product.getImageUrl();
    }

    /** The shop's uploaded UPI QR, or {@code null} when it has none. */
    @Nullable String upiQrUrl(Shop shop) {
        StoredPhoto qr = shop.getUpiQr();
        return qr != null ? mediaUrlSigner.sign(SHOP_QR_KIND, qr.fileName()) : null;
    }
}
