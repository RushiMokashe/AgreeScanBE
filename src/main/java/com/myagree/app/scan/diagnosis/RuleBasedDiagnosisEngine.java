package com.myagree.app.scan.diagnosis;

import org.springframework.stereotype.Component;

import com.myagree.app.plot.Crop;

/**
 * Deterministic demo engine: the diagnosis depends only on the selected crop and scan mode, never on the
 * photo's pixels. See {@link DiagnosisEngine} for how a real model replaces it.
 */
@Component
class RuleBasedDiagnosisEngine implements DiagnosisEngine {

    static final String MODEL_VERSION = "Neural AI Vision v4.2";

    /** Without real vision, "Auto Detect" assumes the district's dominant horticulture crop. */
    static final Crop AUTO_DETECTED_CROP = Crop.TOMATO;

    private final DiseaseKnowledgeBase knowledgeBase;

    RuleBasedDiagnosisEngine(DiseaseKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    @Override
    public String modelVersion() {
        return MODEL_VERSION;
    }

    @Override
    public Diagnosis diagnose(DiagnosisRequest request) {
        Crop crop = request.cropHint() != null ? request.cropHint() : AUTO_DETECTED_CROP;
        return new Diagnosis(crop, knowledgeBase.profileFor(crop, request.mode()));
    }
}
