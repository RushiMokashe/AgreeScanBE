package com.myagree.app.common.media;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A photo kept by {@link PhotoStore}, stored with the record it shows.
 *
 * @param fileName    the generated name, also the id in its signed media URL
 * @param contentType the uploaded media type, e.g. "image/jpeg"
 */
@Embeddable
public record StoredPhoto(
        @Column(name = "photo_file_name", length = 36) String fileName,
        @Column(name = "photo_content_type", length = 100) String contentType) {
}
