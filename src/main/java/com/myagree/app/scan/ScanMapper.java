package com.myagree.app.scan;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import com.myagree.app.plot.Crop;
import com.myagree.app.scan.diagnosis.OrganicAlternative;
import com.myagree.app.scan.diagnosis.Prescription;
import com.myagree.app.scan.dto.CropOptionResponse;
import com.myagree.app.scan.dto.OrganicAlternativeResponse;
import com.myagree.app.scan.dto.PrescriptionResponse;
import com.myagree.app.scan.dto.ScanContextResponse;
import com.myagree.app.scan.dto.ScanDetailResponse;
import com.myagree.app.scan.dto.ScanSummaryResponse;
import com.myagree.app.weather.FieldConditions;

final class ScanMapper {

    /** Uploaded photos are streamed by {@link ScanController#photo(long)}. */
    private static final String UPLOADED_PHOTO_URL = "/api/scans/%d/image";

    private ScanMapper() {
    }

    static ScanSummaryResponse toSummary(Scan scan) {
        return new ScanSummaryResponse(
                scan.getId(),
                scan.getCropLabel(),
                scan.getDiseaseName(),
                scan.isHealthy(),
                scan.getConfidence(),
                imageUrl(scan),
                scan.getScannedAt(),
                scan.getPrescriptionShort(),
                scan.getNote(),
                scan.getStockistCount(),
                scan.getPlotId());
    }

    static ScanDetailResponse toDetail(Scan scan) {
        return new ScanDetailResponse(
                toSummary(scan),
                scan.getCrop().scientificName(),
                scan.getPathogen(),
                scan.getSymptom(),
                scan.getFinding(),
                scan.getSeverity(),
                scan.getSeverityStage(),
                scan.getSeverityLabel(),
                scan.getPlotLabel(),
                scan.getVerifiedBy(),
                scan.getCause(),
                scan.getClimateTrigger(),
                scan.getAudioLanguages(),
                scan.getAudioDurationSeconds(),
                toResponse(scan.getPrescription()),
                toResponse(scan.getOrganicAlternative()),
                scan.getSanitationSteps(),
                scan.getTreatmentPlanId());
    }

    static ScanContextResponse toContext(String modelVersion, FieldConditions field, @Nullable ScanDetailResponse latestScan) {
        return new ScanContextResponse(
                modelVersion,
                Arrays.stream(Crop.values()).map(ScanMapper::toCropOption).toList(),
                field.moisturePercent(),
                field.moistureNote(),
                field.nearbyOutbreakCount(),
                field.nearbyOutbreakNote(),
                latestScan);
    }

    private static String imageUrl(Scan scan) {
        ScanImage image = scan.getImage();
        return image.isUploaded() ? UPLOADED_PHOTO_URL.formatted(scan.getId()) : image.staticUrl();
    }

    private static CropOptionResponse toCropOption(Crop crop) {
        return new CropOptionResponse(crop.name(), crop.label(), crop.emoji());
    }

    private static @Nullable PrescriptionResponse toResponse(@Nullable Prescription prescription) {
        if (prescription == null) {
            return null;
        }
        return new PrescriptionResponse(
                prescription.category(),
                prescription.recommended(),
                prescription.activeIngredient(),
                prescription.marketBrands(),
                prescription.dilution(),
                prescription.dilutionNote(),
                prescription.tankSize(),
                prescription.tankDose(),
                prescription.tankNote(),
                prescription.applicationWindow(),
                prescription.preHarvestIntervalDays(),
                prescription.preHarvestNote());
    }

    private static @Nullable OrganicAlternativeResponse toResponse(@Nullable OrganicAlternative alternative) {
        return alternative == null
                ? null
                : new OrganicAlternativeResponse(alternative.summary(), alternative.note(), alternative.badge());
    }
}
