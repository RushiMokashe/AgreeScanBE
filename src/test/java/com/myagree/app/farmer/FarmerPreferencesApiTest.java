package com.myagree.app.farmer;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

/** The hub and market each farmer chooses, which their rentals and mandi screens open on. */
@AgriScanApiTest
class FarmerPreferencesApiTest {

    private static final String PREFERENCES = "/api/farmer/me/preferences";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void aChosenHubIsWhereTheRentalsScreenOpens() throws Exception {
        long puneHub = JsonBodies.readId(mvc.perform(get("/api/rentals/hubs").with(users.demoFarmer())).andReturn(),
                "$[1].id");
        mvc.perform(get("/api/farmer/me").with(users.demoFarmer()))
                .andExpect(jsonPath("$.preferences.rentalHubId").value(nullValue()));

        mvc.perform(choose("{\"rentalHubId\": %d}".formatted(puneHub)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences.rentalHubId").value(puneHub))
                .andExpect(jsonPath("$.preferences.mandiMarketId").value(nullValue()));
        mvc.perform(get("/api/rentals").with(users.demoFarmer()))
                .andExpect(jsonPath("$.hubName").value("Pune Market Yard Hub"));
        mvc.perform(get("/api/rentals").with(users.secondFarmer()))
                .andExpect(jsonPath("$.hubName").value("Solapur APMC Hub"));
        mvc.perform(choose("{}")).andExpect(jsonPath("$.preferences.rentalHubId").value(puneHub));
    }

    @Test
    void unknownChoicesAreRejected() throws Exception {
        mvc.perform(choose("{\"rentalHubId\": 999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("There is no rental hub 999"));
        mvc.perform(choose("{\"mandiMarketId\": 999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("There is no mandi market 999"));
        mvc.perform(put(PREFERENCES).with(users.owner()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder choose(String json) {
        return put(PREFERENCES).with(users.demoFarmer()).contentType(MediaType.APPLICATION_JSON).content(json);
    }
}
