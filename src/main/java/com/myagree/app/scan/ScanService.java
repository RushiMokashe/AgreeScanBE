package com.myagree.app.scan;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.myagree.app.care.CareService;
import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.NotFoundException;
import com.myagree.app.farmer.FarmerService;
import com.myagree.app.plot.Crop;
import com.myagree.app.plot.PlotLink;
import com.myagree.app.plot.PlotService;
import com.myagree.app.scan.diagnosis.Diagnosis;
import com.myagree.app.scan.diagnosis.DiagnosisEngine;
import com.myagree.app.scan.diagnosis.DiagnosisRequest;
import com.myagree.app.scan.diagnosis.ScanMode;
import com.myagree.app.scan.dto.ScanContextResponse;
import com.myagree.app.scan.dto.ScanDetailResponse;
import com.myagree.app.scan.dto.ScanSummaryResponse;
import com.myagree.app.weather.FieldConditions;
import com.myagree.app.weather.WeatherService;

@Service
@Transactional(readOnly = true)
public class ScanService {

    private static final String AUTO_DETECT = "AUTO";
    private static final ScanMode DEFAULT_MODE = ScanMode.LEAF;
    /** Shown for scans submitted without a photo (e.g. from the desktop demo). */
    private static final String SAMPLE_PHOTO_URL = "/images/scans/scanner-tomato-leaf.jpg";
    private static final String IMAGE_CONTENT_TYPE_PREFIX = "image/";
    /** SVG can carry scripts and is never a camera photo, so it is not accepted as an "image". */
    private static final String SVG_CONTENT_TYPE_PREFIX = "image/svg";

    private final ScanRepository scanRepository;
    private final ScanImageStorage imageStorage;
    private final DiagnosisEngine diagnosisEngine;
    private final PlotService plotService;
    private final CareService careService;
    private final FarmerService farmerService;
    private final WeatherService weatherService;
    private final Clock clock;

    public ScanService(ScanRepository scanRepository, ScanImageStorage imageStorage, DiagnosisEngine diagnosisEngine,
                       PlotService plotService, CareService careService, FarmerService farmerService,
                       WeatherService weatherService, Clock clock) {
        this.scanRepository = scanRepository;
        this.imageStorage = imageStorage;
        this.diagnosisEngine = diagnosisEngine;
        this.plotService = plotService;
        this.careService = careService;
        this.farmerService = farmerService;
        this.weatherService = weatherService;
        this.clock = clock;
    }

    /** The farmer's most recent scans, newest first. */
    public List<ScanSummaryResponse> recentScans(int limit) {
        return scanRepository.findByOrderByScannedAtDescIdDesc(Limit.of(limit)).stream()
                .map(ScanMapper::toSummary)
                .toList();
    }

    /** Full diagnosis and prescription of one scan. */
    public ScanDetailResponse getScan(long scanId) {
        return scanRepository.findWithSanitationStepsById(scanId)
                .map(ScanMapper::toDetail)
                .orElseThrow(() -> NotFoundException.of("Scan", scanId));
    }

    /** The most recent scan in full. */
    public ScanDetailResponse latestScan() {
        return findLatestScan().orElseThrow(() -> new NotFoundException("No scans have been recorded yet"));
    }

    /** Everything the scanner screen shows before the farmer takes a photo. */
    public ScanContextResponse scanContext() {
        FieldConditions fieldConditions = weatherService.fieldConditions(farmerService.currentFarmer().location());
        return ScanMapper.toContext(diagnosisEngine.modelVersion(), fieldConditions, findLatestScan().orElse(null));
    }

    /**
     * Diagnoses a new scan and files it under the plot growing that crop (and the plot's running treatment
     * plan), so the dashboard and treatment tracker pick it up.
     *
     * @param photo    the uploaded image, or {@code null} to use a sample photo
     * @param cropCode a {@link Crop} code; {@code null}, blank or "AUTO" lets the engine detect the crop
     * @param mode     leaf or pest scan; defaults to {@link ScanMode#LEAF}
     * @throws BadRequestException for an unknown crop code or a non-image upload
     */
    @Transactional
    public ScanDetailResponse createScan(@Nullable MultipartFile photo, @Nullable String cropCode, @Nullable ScanMode mode) {
        Crop cropHint = parseCropHint(cropCode);
        ScanMode scanMode = mode != null ? mode : DEFAULT_MODE;
        if (photo != null) {
            requireCameraImage(photo);
        }

        Diagnosis diagnosis = diagnosisEngine.diagnose(
                new DiagnosisRequest(cropHint, scanMode, photo != null ? photo.getResource() : null));
        int stockistCount = diagnosis.hasPrescription() ? careService.countNearbyStockists() : 0;
        Scan scan = new Scan(diagnosis, scanMode, clock.instant(), storeImage(photo), stockistCount);

        Optional<PlotLink> plot = plotService.findPlotGrowing(diagnosis.crop());
        plot.ifPresent(scan::linkToPlot);
        scanRepository.save(scan);
        plot.ifPresent(link -> plotService.recordLatestScan(link.plotId(), scan.getId()));
        return ScanMapper.toDetail(scan);
    }

    /**
     * The photo a farmer uploaded with a scan.
     *
     * @throws NotFoundException when the scan does not exist or uses a bundled sample photo
     */
    public ScanPhoto uploadedPhoto(long scanId) {
        ScanImage image = scanRepository.findById(scanId)
                .orElseThrow(() -> NotFoundException.of("Scan", scanId))
                .getImage();
        return Optional.of(image)
                .filter(ScanImage::isUploaded)
                .flatMap(uploaded -> imageStorage.load(uploaded.storedFileName()))
                .map(content -> new ScanPhoto(content, image.contentType()))
                .orElseThrow(() -> new NotFoundException("Scan %d has no uploaded photo".formatted(scanId)));
    }

    private Optional<ScanDetailResponse> findLatestScan() {
        return scanRepository.findFirstByOrderByScannedAtDescIdDesc().map(ScanMapper::toDetail);
    }

    private ScanImage storeImage(@Nullable MultipartFile photo) {
        return photo != null
                ? ScanImage.uploaded(imageStorage.store(photo), photo.getContentType())
                : ScanImage.staticAsset(SAMPLE_PHOTO_URL);
    }

    private static @Nullable Crop parseCropHint(@Nullable String cropCode) {
        if (cropCode == null || cropCode.isBlank() || AUTO_DETECT.equalsIgnoreCase(cropCode.strip())) {
            return null;
        }
        return Crop.fromCode(cropCode).orElseThrow(() -> new BadRequestException(
                "Unsupported crop '%s'. Use %s or one of %s".formatted(cropCode, AUTO_DETECT, Arrays.toString(Crop.values()))));
    }

    private static void requireCameraImage(MultipartFile photo) {
        if (photo.isEmpty()) {
            throw new BadRequestException("The uploaded photo is empty");
        }
        String contentType = String.valueOf(photo.getContentType()).toLowerCase(Locale.ROOT);
        if (!contentType.startsWith(IMAGE_CONTENT_TYPE_PREFIX) || contentType.startsWith(SVG_CONTENT_TYPE_PREFIX)) {
            throw new BadRequestException("Only photo uploads are supported, but got '%s'".formatted(photo.getContentType()));
        }
    }
}
