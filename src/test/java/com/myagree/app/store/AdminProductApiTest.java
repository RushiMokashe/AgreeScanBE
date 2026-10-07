package com.myagree.app.store;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

/** The admin portal's product prices. */
@AgriScanApiTest
class AdminProductApiTest {

    private static final String PRODUCTS = "/api/admin/products";
    private static final String TOMATO_SEEDS_BARCODE = "8904567000010";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void adminsSeeEveryProduct() throws Exception {
        mvc.perform(get(PRODUCTS).with(users.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(16))
                .andExpect(jsonPath("$[0].name").value("Syngenta Abhinav Tomato Seeds"))
                .andExpect(jsonPath("$[0].price").value(480))
                .andExpect(jsonPath("$[0].mrp").value(550))
                .andExpect(jsonPath("$[0].flashDeal").value(true))
                .andExpect(jsonPath("$[0].barcode").value("8904567000010"))
                .andExpect(jsonPath("$[8].mrp").value(nullValue()))
                .andExpect(jsonPath("$[13].inStock").value(false));
        mvc.perform(get(PRODUCTS).with(users.admin()).header(HttpHeaders.ACCEPT_LANGUAGE, "mr"))
                .andExpect(jsonPath("$[0].name").value("सिंजेंटा अभिनव टोमॅटो बियाणे"));
    }

    @Test
    void adminsChangePricesDealsAndStock() throws Exception {
        mvc.perform(update("{\"price\": 450}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(450))
                .andExpect(jsonPath("$.mrp").value(550));
        mvc.perform(update("{\"mrp\": null, \"flashDeal\": false, \"inStock\": false}"))
                .andExpect(jsonPath("$.price").value(450))
                .andExpect(jsonPath("$.mrp").value(nullValue()))
                .andExpect(jsonPath("$.flashDeal").value(false))
                .andExpect(jsonPath("$.inStock").value(false));
        mvc.perform(get("/api/store").with(users.demoFarmer()))
                .andExpect(jsonPath("$.flashDeals.length()").value(3));
    }

    @Test
    void invalidPricesAndOtherRolesAreRefused() throws Exception {
        mvc.perform(update("{\"mrp\": 400}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The MRP cannot be lower than the selling price"));
        mvc.perform(update("{\"price\": 0}")).andExpect(status().isBadRequest());
        mvc.perform(update("{\"mrp\": 0}")).andExpect(status().isBadRequest());
        mvc.perform(patch(PRODUCTS + "/999").with(users.admin()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(get(PRODUCTS).with(users.demoFarmer())).andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder update(String json) throws Exception {
        long tomatoSeeds = JsonBodies.readId(mvc.perform(get("/api/store/products/barcode/{code}", TOMATO_SEEDS_BARCODE)
                .with(users.demoFarmer())).andReturn(), "$.id");
        return patch(PRODUCTS + "/{id}", tomatoSeeds).with(users.admin()).contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }
}
