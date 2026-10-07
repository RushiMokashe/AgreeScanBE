package com.myagree.app.rental;

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

/** The admin portal's rental rates. */
@AgriScanApiTest
class AdminRentalApiTest {

    private static final String RENTALS = "/api/admin/rentals";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void adminsSeeEveryVehicleWithItsOwnerAndHub() throws Exception {
        mvc.perform(get(RENTALS).with(users.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].name").value("Mahindra Bolero Pickup"))
                .andExpect(jsonPath("$[0].ownerName").value("AgriScan Logistics"))
                .andExpect(jsonPath("$[0].ownerPhone").value("9000000009"))
                .andExpect(jsonPath("$[0].hubName").value("Solapur APMC Hub"))
                .andExpect(jsonPath("$[1].imageUrl").value("/images/rentals/mahindra-575-rotavator.jpg"))
                .andExpect(jsonPath("$[9].online").value(false));
        mvc.perform(get(RENTALS).with(users.admin()).header(HttpHeaders.ACCEPT_LANGUAGE, "mr"))
                .andExpect(jsonPath("$[1].name").value("महिंद्रा 575 DI (45 HP) + रोटाव्हेटर"))
                .andExpect(jsonPath("$[9].hubName").value("लासलगाव (नाशिक) हब"));
    }

    @Test
    void adminsCorrectRatesAndSwitchVehiclesOff() throws Exception {
        long rotavator = JsonBodies.readId(mvc.perform(get(RENTALS).with(users.admin())).andReturn(), "$[1].id");

        mvc.perform(update(rotavator, "{\"rate\": 800}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(800))
                .andExpect(jsonPath("$.rateUnit").value("HOUR"))
                .andExpect(jsonPath("$.online").value(true));
        mvc.perform(update(rotavator, "{\"rateUnit\": \"DAY\", \"online\": false}"))
                .andExpect(jsonPath("$.rate").value(800))
                .andExpect(jsonPath("$.rateUnit").value("DAY"))
                .andExpect(jsonPath("$.online").value(false));
        mvc.perform(get("/api/rentals").with(users.demoFarmer()))
                .andExpect(jsonPath("$.listings[?(@.id == %d)]".formatted(rotavator)).isEmpty());
    }

    @Test
    void invalidUpdatesAndOtherRolesAreRefused() throws Exception {
        mvc.perform(update(1, "{\"rate\": 0}")).andExpect(status().isBadRequest());
        mvc.perform(update(999, "{\"rate\": 500}")).andExpect(status().isNotFound());
        mvc.perform(get(RENTALS).with(users.demoFarmer())).andExpect(status().isForbidden());
        mvc.perform(get(RENTALS).with(users.owner())).andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder update(long id, String json) {
        return patch(RENTALS + "/{id}", id).with(users.admin()).contentType(MediaType.APPLICATION_JSON).content(json);
    }
}
