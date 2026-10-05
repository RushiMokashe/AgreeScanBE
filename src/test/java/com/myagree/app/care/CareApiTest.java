package com.myagree.app.care;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

@AgriScanApiTest
class CareApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void nearbyStockListsHubDealersNearestFirstAndTheOnlineOffer() throws Exception {
        long rxBundleProductId = JsonBodies.readId(
                mvc.perform(get("/api/store").with(users.demoFarmer())).andReturn(), "$.rxBundle.productId");

        mvc.perform(get("/api/care/nearby-stock").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hubName").value("Kem & Karmala Agro Hub"))
                .andExpect(jsonPath("$.district").value("Solapur Dist."))
                .andExpect(jsonPath("$.radiusKm").value(5))
                .andExpect(jsonPath("$.productsLabel").value("Mancozeb & Azoxystrobin"))
                .andExpect(jsonPath("$.mapImageUrl").value("/images/maps/kem-karmala-agro-hub.png"))
                .andExpect(jsonPath("$.onlineOffer.label").value("Buy Online from AgriScan Mandi Depot"))
                .andExpect(jsonPath("$.onlineOffer.price").value(420))
                .andExpect(jsonPath("$.onlineOffer.productId").value(rxBundleProductId))
                .andExpect(jsonPath("$.dealers.length()").value(2))
                .andExpect(jsonPath("$.dealers[0].name").value("Kisan Krishi Seva Kendra"))
                .andExpect(jsonPath("$.dealers[0].certification").value("GOVT_CERTIFIED"))
                .andExpect(jsonPath("$.dealers[0].distanceKm").value(1.8))
                .andExpect(jsonPath("$.dealers[0].reviewCount").value(140))
                .andExpect(jsonPath("$.dealers[0].stock.priceLabel").value("₹280 / 500g"))
                .andExpect(jsonPath("$.dealers[0].stock.note").value("Batch No: IND-2024 • Expiry: Aug 2026"))
                .andExpect(jsonPath("$.dealers[1].name").value("Balaji Agro Chemicals & Seeds"))
                .andExpect(jsonPath("$.dealers[1].certification").value("AUTHORIZED_RETAILER"))
                .andExpect(jsonPath("$.dealers[1].distanceKm").value(3.4))
                .andExpect(jsonPath("$.dealers[1].stock.headline").value("In Stock • Organic Bio-fungicide"));
    }

    @Test
    void agronomistOnDutyIsDrSureshPatel() throws Exception {
        mvc.perform(get("/api/care/agronomist").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dr. Suresh Patel"))
                .andExpect(jsonPath("$.title").value("Plant Pathologist • ICAR Certified"))
                .andExpect(jsonPath("$.photoUrl").value("/images/people/dr-suresh-patel.jpg"))
                .andExpect(jsonPath("$.phone").value("1800999888"))
                .andExpect(jsonPath("$.onDuty").value(true));
    }
}
