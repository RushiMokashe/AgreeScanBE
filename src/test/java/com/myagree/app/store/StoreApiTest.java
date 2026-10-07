package com.myagree.app.store;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.TestUsers;

@AgriScanApiTest
class StoreApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void storeHomeShowsDepotRxBundleDepartmentsAndFlashDeals() throws Exception {
        mvc.perform(get("/api/store").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depotName").value("Solapur Mandi Agro Depot"))
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.deliveryLabel").value("24h Express"))
                .andExpect(jsonPath("$.rxBundle.label").value("Plot A • Blight Triage Rx"))
                .andExpect(jsonPath("$.rxBundle.name").value("Mancozeb 75% WP + Nozzle Kit"))
                .andExpect(jsonPath("$.rxBundle.price").value(420))
                .andExpect(jsonPath("$.rxBundle.mrp").value(530))
                .andExpect(jsonPath("$.rxBundle.scanId").isNumber())
                .andExpect(jsonPath("$.rxBundle.prescribedAt").value("2026-09-29T01:30:00Z"))
                .andExpect(jsonPath("$.rxCount").value(1))
                .andExpect(jsonPath("$.categories.length()").value(4))
                .andExpect(jsonPath("$.categories[0].category").value("CROP_MEDICINE"))
                .andExpect(jsonPath("$.categories[0].localTitle").value("फसल सुरक्षा व कीटनाशक"))
                .andExpect(jsonPath("$.categories[3].title").value("Pashu Palan"))
                .andExpect(jsonPath("$.flashDealEndsAt").value("2026-09-29T13:12:15Z"))
                .andExpect(jsonPath("$.flashDeals.length()").value(4))
                .andExpect(jsonPath("$.flashDeals[0].shortName").value("Syngenta Abhinav Seeds"))
                .andExpect(jsonPath("$.flashDeals[0].tagTone").value("success"))
                .andExpect(jsonPath("$.flashDeals[0].imageBadge").doesNotExist())
                .andExpect(jsonPath("$.flashDeals[1].imageBadge").value("28% OFF"))
                .andExpect(jsonPath("$.flashDeals[1].imageBadgeTone").value("error"))
                .andExpect(jsonPath("$.flashDeals[2].footerTone").value("warning"))
                .andExpect(jsonPath("$.flashDeals[3].shortName").value("Tata Contaf Plus 500ml"));
    }

    @Test
    void productsWithoutFiltersReturnTheWholeCatalogue() throws Exception {
        mvc.perform(get("/api/store/products").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(16))
                .andExpect(jsonPath("$[4].shortName").value("Mancozeb 500g"))
                .andExpect(jsonPath("$[4].flashDeal").value(false));
    }

    @Test
    void productsCanBeFilteredByCategory() throws Exception {
        mvc.perform(get("/api/store/products").param("category", "CROP_MEDICINE").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].name").value("Tata Contaf Plus (Hexaconazole 5% SC)"))
                .andExpect(jsonPath("$[2].shortName").value("Plot A Blight Rx Bundle"));
    }

    @Test
    void productSearchIsCaseInsensitive() throws Exception {
        mvc.perform(get("/api/store/products").param("q", "MANCOZEB").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Indofil M-45 Mancozeb 75% WP"));
    }

    @Test
    void categoryAndSearchCombine() throws Exception {
        mvc.perform(get("/api/store/products").param("category", "SEEDS").param("q", "tomato").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Syngenta Abhinav Tomato Seeds"));
    }

    @Test
    void searchTreatsWildcardCharactersLiterally() throws Exception {
        mvc.perform(get("/api/store/products").param("q", "%").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void unknownCategoryIsRejected() throws Exception {
        mvc.perform(get("/api/store/products").param("category", "FERTILIZER").with(users.demoFarmer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Invalid value 'FERTILIZER' for 'category'. Allowed values: CROP_MEDICINE, SEEDS, TOOLS, DAIRY"));
    }

    @Test
    void theStoreIsShownInTheReadersLanguage() throws Exception {
        mvc.perform(get("/api/store").with(users.demoFarmer()).header(HttpHeaders.ACCEPT_LANGUAGE, "mr"))
                .andExpect(jsonPath("$.deliveryLabel").value("24 तासांत डिलिव्हरी"))
                .andExpect(jsonPath("$.categories[0].title").value("पीक औषधे"))
                .andExpect(jsonPath("$.rxBundle.label").value("प्लॉट A • करपा उपचार"))
                .andExpect(jsonPath("$.flashDeals[0].name").value("सिंजेंटा अभिनव टोमॅटो बियाणे"));
        mvc.perform(get("/api/store/products").param("q", "मैन्कोज़ेब").with(users.demoFarmer())
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "hi"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].shortName").value("मैन्कोज़ेब 500 ग्राम"));
    }

    @Test
    void eachFarmerSeesOnlyTheirOwnPrescription() throws Exception {
        mvc.perform(get("/api/store").with(users.secondFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rxBundle").value(nullValue()))
                .andExpect(jsonPath("$.rxCount").value(0));
    }

    @Test
    void productsAreFoundByTheirBarcode() throws Exception {
        mvc.perform(get("/api/store/products/barcode/{code}", "8904567000058").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortName").value("Mancozeb 500g"))
                .andExpect(jsonPath("$.barcode").value("8904567000058"))
                .andExpect(jsonPath("$.inStock").value(true));
        mvc.perform(get("/api/store/products/barcode/{code}", "8904567000140").with(users.demoFarmer()))
                .andExpect(jsonPath("$.inStock").value(false))
                .andExpect(jsonPath("$.imageUrl").value(nullValue()));
        mvc.perform(get("/api/store/products/barcode/{code}", "8904567000059").with(users.demoFarmer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("8904567000059 is not an EAN-13 barcode: 13 digits ending in the check digit"));
        mvc.perform(get("/api/store/products/barcode/{code}", "4006381333931").with(users.demoFarmer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No product has the barcode 4006381333931"));
    }
}
