package com.myagree.app.scan;

import jakarta.persistence.Embeddable;

import org.jspecify.annotations.Nullable;

/**
 * Where a scan's photo lives: a static asset bundled with the frontend, or a file the farmer uploaded.
 * Exactly one of {@code staticUrl} and {@code storedFileName} is set.
 */
@Embeddable
public record ScanImage(@Nullable String staticUrl, @Nullable String storedFileName, @Nullable String contentType) {

    public static ScanImage staticAsset(String url) {
        return new ScanImage(url, null, null);
    }

    public static ScanImage uploaded(String storedFileName, String contentType) {
        return new ScanImage(null, storedFileName, contentType);
    }

    public boolean isUploaded() {
        return storedFileName != null;
    }
}
