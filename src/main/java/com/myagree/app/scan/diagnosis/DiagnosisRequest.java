package com.myagree.app.scan.diagnosis;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;

import com.myagree.app.plot.Crop;

/**
 * @param cropHint the crop the farmer selected, or {@code null} for "Auto Detect"
 * @param mode     leaf (disease) or pest scan
 * @param photo    the uploaded photo, or {@code null} when the farmer scanned without one
 */
public record DiagnosisRequest(@Nullable Crop cropHint, ScanMode mode, @Nullable Resource photo) {
}
