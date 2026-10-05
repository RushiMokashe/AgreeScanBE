package com.myagree.app.scan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.FixedClockConfiguration;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

@AgriScanApiTest
class ScanApiTest {

    private static final byte[] PHOTO_BYTES = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void scanContextDescribesTheScannerScreen() throws Exception {
        mvc.perform(get("/api/scans/context").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelVersion").value("Neural AI Vision v4.2"))
                .andExpect(jsonPath("$.supportedCrops.length()").value(6))
                .andExpect(jsonPath("$.supportedCrops[0].code").value("TOMATO"))
                .andExpect(jsonPath("$.supportedCrops[0].emoji").value("🍅"))
                .andExpect(jsonPath("$.supportedCrops[2].label").value("Rice / Paddy"))
                .andExpect(jsonPath("$.supportedCrops[5].code").value("SUGARCANE"))
                .andExpect(jsonPath("$.fieldMoisturePercent").value(78))
                .andExpect(jsonPath("$.fieldMoistureNote").value("Optimal for Fungal Check"))
                .andExpect(jsonPath("$.nearbyOutbreakCount").value(3))
                .andExpect(jsonPath("$.nearbyOutbreakNote").value("Blight within 5 km"))
                .andExpect(jsonPath("$.latestScan.symptom").value("Concentric brown ring lesions (Alternaria spp.)"))
                .andExpect(jsonPath("$.latestScan.finding").value("Early Blight pattern detected on lower foliage"));
    }

    @Test
    void latestScanCarriesTheFullDiagnosisAndPrescription() throws Exception {
        mvc.perform(get("/api/scans/latest").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.cropLabel").value("Tomato"))
                .andExpect(jsonPath("$.imageUrl").value("/images/scans/early-blight-hero.jpg"))
                .andExpect(jsonPath("$.cropScientificName").value("Solanum lycopersicum"))
                .andExpect(jsonPath("$.pathogen").value("Alternaria solani"))
                .andExpect(jsonPath("$.severity").value("SEVERE"))
                .andExpect(jsonPath("$.severityStage").value(2))
                .andExpect(jsonPath("$.severityLabel").value("Severe Risk • Spreading"))
                .andExpect(jsonPath("$.plotLabel").value("North Field • Plot A"))
                .andExpect(jsonPath("$.verifiedBy").value("AI Verified Agronomist"))
                .andExpect(jsonPath("$.climateTrigger").value("27°C with 86% leaf wetness"))
                .andExpect(jsonPath("$.audioLanguages").value("Hindi, English"))
                .andExpect(jsonPath("$.audioDurationSeconds").value(42))
                .andExpect(jsonPath("$.prescription.activeIngredient").value("Mancozeb 75% WP"))
                .andExpect(jsonPath("$.prescription.tankDose").value("38 - 40 Grams"))
                .andExpect(jsonPath("$.prescription.preHarvestIntervalDays").value(7))
                .andExpect(jsonPath("$.prescription.preHarvestNote")
                        .value("Wait strictly **7 days** before harvesting any mature tomatoes."))
                .andExpect(jsonPath("$.organicAlternative.badge").value("Residue Free"))
                .andExpect(jsonPath("$.sanitationSteps.length()").value(2))
                .andExpect(jsonPath("$.treatmentPlanId").isNumber());
    }

    @Test
    void recentScansHonourTheLimit() throws Exception {
        mvc.perform(get("/api/scans").param("limit", "1").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].diseaseName").value("Early Blight"));
    }

    @Test
    void scanWithPhotoIsDiagnosedFiledUnderItsPlotAndServesThePhoto() throws Exception {
        MockMultipartFile photo = new MockMultipartFile("image", "leaf.png", MediaType.IMAGE_PNG_VALUE, PHOTO_BYTES);

        MvcResult created = mvc.perform(multipart("/api/scans").file(photo).param("crop", "TOMATO")
                        .with(users.demoFarmer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diseaseName").value("Early Blight"))
                .andExpect(jsonPath("$.scannedAt").value(FixedClockConfiguration.NOW.toString()))
                .andExpect(jsonPath("$.plotLabel").value("North Field • Plot A"))
                .andExpect(jsonPath("$.treatmentPlanId").isNumber())
                .andExpect(jsonPath("$.stockistCount").value(2))
                .andReturn();
        long scanId = JsonBodies.readId(created, "$.id");

        assertThat(JsonBodies.<String>read(created, "$.imageUrl")).isEqualTo("/api/scans/" + scanId + "/image");
        mvc.perform(get("/api/scans/{id}/image", scanId).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PHOTO_BYTES));
    }

    @Test
    void newScanBecomesThePlotsLatestScan() throws Exception {
        MvcResult created = mvc.perform(multipart("/api/scans").param("crop", "TOMATO").with(users.demoFarmer()))
                .andExpect(status().isCreated())
                .andReturn();

        mvc.perform(get("/api/dashboard").with(users.demoFarmer()))
                .andExpect(jsonPath("$.plots[0].latestScanId").value(JsonBodies.readId(created, "$.id")))
                .andExpect(jsonPath("$.recentScans.length()").value(3));
    }

    @Test
    void scanWithoutPhotoOrCropAutoDetectsWithTheSamplePhoto() throws Exception {
        mvc.perform(multipart("/api/scans").with(users.demoFarmer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cropLabel").value("Tomato"))
                .andExpect(jsonPath("$.imageUrl").value("/images/scans/scanner-tomato-leaf.jpg"));
    }

    @Test
    void cottonScanInPestModeFindsWhitefly() throws Exception {
        mvc.perform(multipart("/api/scans").param("crop", "COTTON").param("mode", "PEST").with(users.demoFarmer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diseaseName").value("Whitefly Infestation"))
                .andExpect(jsonPath("$.pathogen").value("Bemisia tabaci"))
                .andExpect(jsonPath("$.plotLabel").value("East Field • Plot C"))
                .andExpect(jsonPath("$.treatmentPlanId").value(nullValue()));
    }

    @Test
    void scanOfACropTheFarmerDoesNotGrowIsNotFiledUnderAPlot() throws Exception {
        mvc.perform(multipart("/api/scans").param("crop", "PADDY").with(users.demoFarmer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diseaseName").value("Rice Blast"))
                .andExpect(jsonPath("$.prescription.activeIngredient").value("Tricyclazole 75% WP"))
                .andExpect(jsonPath("$.plotId").value(nullValue()))
                .andExpect(jsonPath("$.plotLabel").value(nullValue()))
                .andExpect(jsonPath("$.treatmentPlanId").value(nullValue()));
    }

    @Test
    void unknownCropIsRejected() throws Exception {
        mvc.perform(multipart("/api/scans").param("crop", "MANGO").with(users.demoFarmer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("MANGO")));
    }

    @Test
    void nonImageUploadIsRejected() throws Exception {
        MockMultipartFile notes = new MockMultipartFile("image", "notes.txt", MediaType.TEXT_PLAIN_VALUE, PHOTO_BYTES);

        mvc.perform(multipart("/api/scans").file(notes).with(users.demoFarmer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", startsWith("Only photo uploads are supported")));
    }

    @Test
    void unknownScanReturnsTheStandardNotFoundBody() throws Exception {
        mvc.perform(get("/api/scans/{id}", 99).with(users.demoFarmer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Scan 99 not found"));
    }

    @Test
    void scanWithASamplePhotoHasNoUploadedImage() throws Exception {
        long seededScanId = JsonBodies.readId(
                mvc.perform(get("/api/scans/latest").with(users.demoFarmer())).andReturn(), "$.id");

        mvc.perform(get("/api/scans/{id}/image", seededScanId).with(users.demoFarmer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Scan " + seededScanId + " has no uploaded photo"));
    }
}
