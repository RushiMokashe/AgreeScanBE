package com.myagree.app.scan;

import java.time.Duration;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.myagree.app.scan.diagnosis.ScanMode;
import com.myagree.app.scan.dto.ScanContextResponse;
import com.myagree.app.scan.dto.ScanDetailResponse;
import com.myagree.app.scan.dto.ScanSummaryResponse;

@RestController
@RequestMapping("/api/scans")
class ScanController {

    private static final String DEFAULT_LIMIT = "10";
    private static final int MAX_LIMIT = 50;
    /** A scan's photo never changes, so browsers may cache it. */
    private static final Duration PHOTO_CACHE_DURATION = Duration.ofDays(7);

    private final ScanService scanService;

    ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @GetMapping
    List<ScanSummaryResponse> recentScans(@RequestParam(defaultValue = DEFAULT_LIMIT) @Min(1) @Max(MAX_LIMIT) int limit) {
        return scanService.recentScans(limit);
    }

    @GetMapping("/latest")
    ScanDetailResponse latestScan() {
        return scanService.latestScan();
    }

    @GetMapping("/context")
    ScanContextResponse scanContext() {
        return scanService.scanContext();
    }

    @GetMapping("/{id}")
    ScanDetailResponse scan(@PathVariable long id) {
        return scanService.getScan(id);
    }

    @GetMapping("/{id}/image")
    ResponseEntity<Resource> photo(@PathVariable long id) {
        ScanPhoto photo = scanService.uploadedPhoto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .cacheControl(CacheControl.maxAge(PHOTO_CACHE_DURATION))
                .body(photo.content());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ScanDetailResponse createScan(@RequestParam(required = false) @Nullable MultipartFile image,
                                  @RequestParam(required = false) @Nullable String crop,
                                  @RequestParam(required = false) @Nullable ScanMode mode) {
        return scanService.createScan(image, crop, mode);
    }
}
