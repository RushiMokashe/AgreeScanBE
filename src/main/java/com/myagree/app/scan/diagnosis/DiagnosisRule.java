package com.myagree.app.scan.diagnosis;

import org.jspecify.annotations.Nullable;

import com.myagree.app.plot.Crop;

/**
 * One entry of the knowledge base: the profile reported for a crop, optionally only in one scan mode.
 *
 * @param mode {@code null} when the rule applies to every scan mode
 */
record DiagnosisRule(Crop crop, @Nullable ScanMode mode, DiseaseProfile profile) {

    boolean matches(Crop requestedCrop, ScanMode requestedMode) {
        return crop == requestedCrop && (mode == null || mode == requestedMode);
    }
}
