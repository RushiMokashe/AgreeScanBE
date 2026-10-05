package com.myagree.app.dashboard;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.TestUsers;

@AgriScanApiTest
class DashboardApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void dashboardShowsFarmerWeatherAlertAndHelpline() throws Exception {
        mvc.perform(get("/api/dashboard").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farmer.name").value("Rishikesh"))
                .andExpect(jsonPath("$.farmer.season").value("Rabi 2024"))
                .andExpect(jsonPath("$.weather.temperatureC").value(29))
                .andExpect(jsonPath("$.weather.conditionIcon").value("partly_cloudy_day"))
                .andExpect(jsonPath("$.weather.humidityPercent").value(68))
                .andExpect(jsonPath("$.weather.rainInDays").value(2))
                .andExpect(jsonPath("$.alerts.length()").value(1))
                .andExpect(jsonPath("$.alerts[0].title").value("High Blight Risk in Karmala Taluka"))
                .andExpect(jsonPath("$.alerts[0].severity").value("HIGH"))
                .andExpect(jsonPath("$.alerts[0].region").value("Karmala Taluka"))
                .andExpect(jsonPath("$.helpline.name").value("Kisan Call Center"))
                .andExpect(jsonPath("$.helpline.phone").value("18001801551"))
                .andExpect(jsonPath("$.helpline.displayNumber").value("1800-180-1551"))
                .andExpect(jsonPath("$.helpline.hours").value("Toll-Free 24/7"));
    }

    @Test
    void dashboardListsTheThreePlotsAsDesigned() throws Exception {
        mvc.perform(get("/api/dashboard").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plots.length()").value(3))
                .andExpect(jsonPath("$.plots[0].code").value("Plot A"))
                .andExpect(jsonPath("$.plots[0].fieldName").value("North Field"))
                .andExpect(jsonPath("$.plots[0].crop").value("Tomato"))
                .andExpect(jsonPath("$.plots[0].variety").value("Arka Rakshak"))
                .andExpect(jsonPath("$.plots[0].areaAcres").value(2.5))
                .andExpect(jsonPath("$.plots[0].health").value("ACTION_NEEDED"))
                .andExpect(jsonPath("$.plots[0].metrics[1].label").value("Last Diagnostic"))
                .andExpect(jsonPath("$.plots[0].metrics[1].value").value("Yesterday (Blight)"))
                .andExpect(jsonPath("$.plots[0].metrics[1].tone").value("error"))
                .andExpect(jsonPath("$.plots[0].pendingAction").value("Fungicide spray pending"))
                .andExpect(jsonPath("$.plots[0].activeTreatmentPlanId").isNumber())
                .andExpect(jsonPath("$.plots[0].latestScanId").isNumber())
                .andExpect(jsonPath("$.plots[1].health").value("HEALTHY"))
                .andExpect(jsonPath("$.plots[1].areaAcres").value(4.0))
                .andExpect(jsonPath("$.plots[1].metrics[1].tone").value("success"))
                .andExpect(jsonPath("$.plots[1].pendingAction").value(nullValue()))
                .andExpect(jsonPath("$.plots[1].latestScanId").value(nullValue()))
                .andExpect(jsonPath("$.plots[2].health").value("MODERATE_RISK"))
                .andExpect(jsonPath("$.plots[2].activeTreatmentPlanId").value(nullValue()))
                .andExpect(jsonPath("$.plots[2].latestScanId").isNumber());
    }

    @Test
    void dashboardListsRecentScansNewestFirst() throws Exception {
        mvc.perform(get("/api/dashboard").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentScans.length()").value(2))
                .andExpect(jsonPath("$.recentScans[0].diseaseName").value("Early Blight"))
                .andExpect(jsonPath("$.recentScans[0].confidence").value(96))
                .andExpect(jsonPath("$.recentScans[0].scannedAt").value("2026-09-28T11:00:00Z"))
                .andExpect(jsonPath("$.recentScans[0].prescriptionShort").value("Mancozeb 75% WP (2.5g/L)"))
                .andExpect(jsonPath("$.recentScans[0].stockistCount").value(2))
                .andExpect(jsonPath("$.recentScans[0].note").value(nullValue()))
                .andExpect(jsonPath("$.recentScans[1].cropLabel").value("Bt Cotton Stem"))
                .andExpect(jsonPath("$.recentScans[1].diseaseName").value("Healthy Foliage"))
                .andExpect(jsonPath("$.recentScans[1].healthy").value(true))
                .andExpect(jsonPath("$.recentScans[1].note").value("No pathogen or fungal trace"))
                .andExpect(jsonPath("$.recentScans[1].prescriptionShort").value(nullValue()))
                .andExpect(jsonPath("$.recentScans[1].scannedAt").value("2026-09-26T04:30:00Z"));
    }

    @Test
    void currentFarmerProfileIsTheDemoFarmer() throws Exception {
        mvc.perform(get("/api/farmer/me").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Rishikesh"))
                .andExpect(jsonPath("$.location").value("Solapur, Maharashtra"))
                .andExpect(jsonPath("$.avatarUrl").value("/images/people/farmer-profile.jpg"))
                .andExpect(jsonPath("$.preferences.rentalHubId").value(nullValue()))
                .andExpect(jsonPath("$.preferences.mandiMarketId").value(nullValue()));
    }

    @Test
    void plotsEndpointReturnsTheSamePlots() throws Exception {
        mvc.perform(get("/api/plots").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[2].code").value("Plot C"))
                .andExpect(jsonPath("$[2].variety").value("Bt Cotton"));
    }
}
