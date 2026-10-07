package com.myagree.app.common.media;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.myagree.app.common.AgriScanProperties;
import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.i18n.UserMessage;

/**
 * Keeps uploaded photos on disk under {@code agriscan.uploads-dir}, one folder per media kind, each under a generated
 * name (never the client's file name). Only raster photos are accepted: an SVG or HTML file could carry script to
 * whoever opens its media URL. A feature stores the returned {@link StoredPhoto} with its record and serves it through
 * its {@link MediaSource} with {@link #load}.
 */
@Component
public class PhotoStore {

    private static final String IMAGE_TYPE_PREFIX = "image/";
    private static final String SVG_TYPE_PREFIX = "image/svg";
    private static final UserMessage PHOTO_EMPTY = UserMessage.of("common.media.photo-empty");
    private static final UserMessage PHOTO_ONLY = UserMessage.of("common.media.photo-only");

    private final Path root;

    PhotoStore(AgriScanProperties properties) {
        this.root = properties.uploadsDir().toAbsolutePath().normalize();
    }

    /**
     * Saves an uploaded photo for media kind {@code kind}.
     *
     * @throws BadRequestException when the upload is empty or not a raster photo
     */
    public StoredPhoto store(String kind, MultipartFile photo) {
        if (photo.isEmpty()) {
            throw new BadRequestException(PHOTO_EMPTY);
        }
        String contentType = String.valueOf(photo.getContentType()).toLowerCase(Locale.ROOT);
        if (!contentType.startsWith(IMAGE_TYPE_PREFIX) || contentType.startsWith(SVG_TYPE_PREFIX)) {
            throw new BadRequestException(PHOTO_ONLY);
        }
        String fileName = UUID.randomUUID().toString();
        try {
            Path directory = Files.createDirectories(root.resolve(kind));
            photo.transferTo(directory.resolve(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot store an uploaded %s photo".formatted(kind), e);
        }
        return new StoredPhoto(fileName, contentType);
    }

    /** The stored photo, or empty when its file is gone or the name points outside the kind's folder. */
    public Optional<MediaContent> load(String kind, StoredPhoto photo) {
        Path directory = root.resolve(kind).normalize();
        Path file = directory.resolve(photo.fileName()).normalize();
        if (!file.startsWith(directory) || !Files.isReadable(file)) {
            return Optional.empty();
        }
        return Optional.of(new MediaContent(new FileSystemResource(file), MediaType.parseMediaType(photo.contentType())));
    }
}
