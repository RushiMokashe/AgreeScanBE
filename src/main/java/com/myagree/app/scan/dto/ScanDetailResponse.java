package com.myagree.app.scan.dto;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.myagree.app.scan.diagnosis.Severity;

/**
 * Mirrors {@code ScanDetail} in frontend/src/lib/types.ts. The contract's {@code ScanDetail extends ScanSummary}
 * is expressed by unwrapping the summary's fields into this object.
 */
public record ScanDetailResponse(
        @JsonUnwrapped ScanSummaryResponse summary,
        @Nullable String cropScientificName,
        @Nullable String pathogen,
        @Nullable String symptom,
        @Nullable String finding,
        Severity severity,
        @Nullable Integer severityStage,
        String severityLabel,
        @Nullable String plotLabel,
        @Nullable String verifiedBy,
        @Nullable String cause,
        @Nullable String climateTrigger,
        @Nullable String audioLanguages,
        @Nullable Integer audioDurationSeconds,
        @Nullable PrescriptionResponse prescription,
        @Nullable OrganicAlternativeResponse organicAlternative,
        List<String> sanitationSteps,
        @Nullable Long treatmentPlanId) {
}
