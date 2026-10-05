package com.myagree.app.scan.diagnosis;

import com.myagree.app.plot.Crop;

/**
 * Result of analysing a crop photo.
 *
 * @param crop    the crop the photo shows; resolved by the engine when the farmer chose "Auto Detect"
 * @param profile the condition found on it
 */
public record Diagnosis(Crop crop, DiseaseProfile profile) {

    public boolean hasPrescription() {
        return profile.prescription() != null;
    }
}
