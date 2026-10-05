package com.myagree.app.scan.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.myagree.app.plot.Crop;

import tools.jackson.databind.json.JsonMapper;

class RuleBasedDiagnosisEngineTest {

    private final RuleBasedDiagnosisEngine engine =
            new RuleBasedDiagnosisEngine(new DiseaseKnowledgeBase(JsonMapper.builder().build()));

    @Test
    void autoDetectAssumesTomatoAndFindsEarlyBlight() {
        Diagnosis diagnosis = engine.diagnose(new DiagnosisRequest(null, ScanMode.LEAF, null));

        assertThat(diagnosis.crop()).isEqualTo(Crop.TOMATO);
        assertThat(diagnosis.profile().diseaseName()).isEqualTo("Early Blight");
        assertThat(diagnosis.profile().prescriptionShort()).isEqualTo("Mancozeb 75% WP (2.5g/L)");
    }

    @ParameterizedTest(name = "{0} in {1} mode -> {2}")
    @CsvSource({
            "TOMATO,    PEST, Early Blight",
            "WHEAT,     LEAF, Yellow Rust",
            "PADDY,     LEAF, Rice Blast",
            "COTTON,    PEST, Whitefly Infestation",
            "COTTON,    LEAF, Healthy Foliage",
            "CHILLI,    LEAF, Leaf Curl Virus",
            "SUGARCANE, LEAF, Red Rot"
    })
    void diagnosisDependsOnCropAndMode(Crop crop, ScanMode mode, String expectedDisease) {
        Diagnosis diagnosis = engine.diagnose(new DiagnosisRequest(crop, mode, null));

        assertThat(diagnosis.crop()).isEqualTo(crop);
        assertThat(diagnosis.profile().diseaseName()).isEqualTo(expectedDisease);
    }

    @Test
    void everyDiseaseComesWithAFullTreatment() {
        for (Crop crop : Crop.values()) {
            for (ScanMode mode : ScanMode.values()) {
                DiseaseProfile profile = engine.diagnose(new DiagnosisRequest(crop, mode, null)).profile();
                if (profile.healthy()) {
                    continue;
                }
                assertThat(profile.pathogen()).as("%s %s pathogen", crop, mode).isNotBlank();
                assertThat(profile.prescription()).as("%s %s prescription", crop, mode).isNotNull();
                assertThat(profile.organicAlternative()).as("%s %s organic alternative", crop, mode).isNotNull();
                assertThat(profile.sanitationSteps()).as("%s %s sanitation steps", crop, mode).isNotEmpty();
            }
        }
    }

    @Test
    void healthyCropHasNoPrescription() {
        DiseaseProfile profile = engine.diagnose(new DiagnosisRequest(Crop.COTTON, ScanMode.LEAF, null)).profile();

        assertThat(profile.healthy()).isTrue();
        assertThat(profile.severity()).isEqualTo(Severity.NONE);
        assertThat(profile.prescription()).isNull();
        assertThat(profile.note()).isEqualTo("No pathogen or fungal trace");
    }
}
