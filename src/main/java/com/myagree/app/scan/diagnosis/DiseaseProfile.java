package com.myagree.app.scan.diagnosis;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Everything AgriScan tells the farmer about one crop condition: what it is, how bad it is, why it
 * happened and how to treat it. Healthy results carry preventive tips in {@code sanitationSteps}.
 */
public record DiseaseProfile(
        String cropLabel,
        String diseaseName,
        boolean healthy,
        int confidence,
        Severity severity,
        @Nullable Integer severityStage,
        String severityLabel,
        @Nullable String pathogen,
        @Nullable String symptom,
        @Nullable String finding,
        @Nullable String note,
        @Nullable String cause,
        @Nullable String climateTrigger,
        @Nullable String verifiedBy,
        @Nullable String audioLanguages,
        @Nullable Integer audioDurationSeconds,
        @Nullable Prescription prescription,
        @Nullable OrganicAlternative organicAlternative,
        List<String> sanitationSteps,
        @Nullable String prescriptionShort) {

    public DiseaseProfile {
        sanitationSteps = List.copyOf(sanitationSteps);
    }
}
