package com.myagree.app.scan;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.myagree.app.common.AgriScanProperties;

/** Keeps uploaded crop photos on disk under {@code agriscan.uploads-dir}. */
@Component
class ScanImageStorage {

    private final Path directory;

    ScanImageStorage(AgriScanProperties properties) {
        this.directory = properties.uploadsDir().toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create uploads directory " + directory, e);
        }
    }

    /** Saves the photo under a generated name (never the client's file name) and returns that name. */
    String store(MultipartFile photo) {
        String fileName = UUID.randomUUID().toString();
        try {
            photo.transferTo(directory.resolve(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot store uploaded photo", e);
        }
        return fileName;
    }

    Optional<Resource> load(String fileName) {
        Path file = directory.resolve(fileName);
        return Files.isReadable(file) ? Optional.of(new FileSystemResource(file)) : Optional.empty();
    }
}
