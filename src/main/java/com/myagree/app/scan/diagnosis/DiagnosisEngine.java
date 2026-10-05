package com.myagree.app.scan.diagnosis;

/**
 * The seam where a real crop-disease vision model plugs in.
 *
 * <p>An implementation receives the farmer's photo (when there is one), the crop they selected and the scan
 * mode, and returns the diagnosis AgriScan stores and shows. The bundled {@link RuleBasedDiagnosisEngine}
 * is a deterministic stand-in driven by {@link DiseaseKnowledgeBase}. A production model (for example a
 * hosted image classifier) replaces it by providing another {@code DiagnosisEngine} bean marked
 * {@code @Primary}; {@code ScanService} and the stored scan format stay unchanged.
 */
public interface DiagnosisEngine {

    /** Model name and version shown on the scanner, e.g. "Neural AI Vision v4.2". */
    String modelVersion();

    /** Diagnoses one scan. Must always return a result; healthy crops get a healthy profile. */
    Diagnosis diagnose(DiagnosisRequest request);
}
