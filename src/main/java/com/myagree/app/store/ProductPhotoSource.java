package com.myagree.app.store;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.myagree.app.common.media.MediaContent;
import com.myagree.app.common.media.MediaSource;
import com.myagree.app.common.media.PhotoStore;

/** Serves shopkeepers' product photos: {@code /api/media/product/{fileName}}. */
@Component
class ProductPhotoSource implements MediaSource {

    private final ProductRepository productRepository;
    private final PhotoStore photoStore;

    ProductPhotoSource(ProductRepository productRepository, PhotoStore photoStore) {
        this.productRepository = productRepository;
        this.photoStore = photoStore;
    }

    @Override
    public String kind() {
        return StorePictures.PRODUCT_KIND;
    }

    /** The photo a product shows now; a photo replaced since is no longer served. */
    @Override
    public Optional<MediaContent> load(String id) {
        return productRepository.findByPhotoFileName(id)
                .flatMap(product -> photoStore.load(StorePictures.PRODUCT_KIND, Objects.requireNonNull(product.getPhoto())));
    }
}
