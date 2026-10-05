package com.myagree.app.rental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.FixedClockConfiguration;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

@AgriScanApiTest
class RentalApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void overviewShowsHubSpotlightAndTheFourListings() throws Exception {
        MvcResult overview = mvc.perform(get("/api/rentals").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hubName").value("Solapur APMC Hub"))
                .andExpect(jsonPath("$.radiusKm").value(12))
                .andExpect(jsonPath("$.routes").value("Karmala, Kem & Kurduwadi routes"))
                .andExpect(jsonPath("$.onlineCount").value(14))
                .andExpect(jsonPath("$.spotlight.title").value("Mahindra Bolero Pickup"))
                .andExpect(jsonPath("$.spotlight.label").value("Mandi Harvest Express • ताजी मंडी रवानगी"))
                .andExpect(jsonPath("$.spotlight.baseFare").value(450))
                .andExpect(jsonPath("$.spotlight.perKmRate").value(22))
                .andExpect(jsonPath("$.spotlight.dispatchMinutes").value(25))
                .andExpect(jsonPath("$.spotlight.perks.length()").value(3))
                .andExpect(jsonPath("$.spotlight.perks[1].title").value("Dharmakanta"))
                .andExpect(jsonPath("$.listings.length()").value(4))
                .andReturn();

        List<Number> listingIds = JsonBodies.read(overview, "$.listings[*].id");
        assertThat(listingIds)
                .extracting(Number::longValue)
                .doesNotContain(JsonBodies.readId(overview, "$.spotlight.listingId"));
    }

    @Test
    void machineryListingsCarryPhotosAndTransportListingsCarryIcons() throws Exception {
        mvc.perform(get("/api/rentals").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listings[0].category").value("MACHINERY"))
                .andExpect(jsonPath("$.listings[0].imageUrl").value("/images/rentals/mahindra-575-rotavator.jpg"))
                .andExpect(jsonPath("$.listings[0].badge").value("Verified Field Tested"))
                .andExpect(jsonPath("$.listings[0].distanceKm").value(2.4))
                .andExpect(jsonPath("$.listings[0].rateUnit").value("HOUR"))
                .andExpect(jsonPath("$.listings[0].operator.initials").value("RP"))
                .andExpect(jsonPath("$.listings[0].specs.length()").value(3))
                .andExpect(jsonPath("$.listings[0].specs[0].value").value("6 Feet"))
                .andExpect(jsonPath("$.listings[1].rateNoteHighlighted").value(false))
                .andExpect(jsonPath("$.listings[1].specs").value(empty()))
                .andExpect(jsonPath("$.listings[2].category").value("TRANSPORT"))
                .andExpect(jsonPath("$.listings[2].imageUrl").value(nullValue()))
                .andExpect(jsonPath("$.listings[2].icon").value("local_shipping"))
                .andExpect(jsonPath("$.listings[2].badge").value(nullValue()))
                .andExpect(jsonPath("$.listings[2].distanceKm").value(nullValue()))
                .andExpect(jsonPath("$.listings[2].rateUnit").value("KM"))
                .andExpect(jsonPath("$.listings[2].features.length()").value(2))
                .andExpect(jsonPath("$.listings[3].bookLabel").value("Reserve 4 AM Run"));
    }

    @Test
    void bookingWithoutASlotTakesTheNextAvailableOne() throws Exception {
        long listingId = listingId(0);

        mvc.perform(post("/api/rentals/{id}/bookings", listingId)
                        .with(users.demoFarmer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.listingId").value(listingId))
                .andExpect(jsonPath("$.listingName").value("Mahindra 575 DI (45 HP) + Rotavator"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.slotLabel").value("Slot: 02:00 PM Today"))
                .andExpect(jsonPath("$.createdAt").value(FixedClockConfiguration.NOW.toString()))
                .andExpect(jsonPath("$.message").value("Booked! Rameshwar Patil will call you shortly to confirm."));
    }

    @Test
    void bookingKeepsTheRequestedSlot() throws Exception {
        mvc.perform(post("/api/rentals/{id}/bookings", listingId(2))
                        .with(users.demoFarmer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotLabel\": \"Tomorrow 5:00 AM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slotLabel").value("Tomorrow 5:00 AM"));
    }

    @Test
    void spotlightListingCanBeBooked() throws Exception {
        long spotlightListingId = JsonBodies.readId(
                mvc.perform(get("/api/rentals").with(users.demoFarmer())).andReturn(), "$.spotlight.listingId");

        mvc.perform(post("/api/rentals/{id}/bookings", spotlightListingId).with(users.demoFarmer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingName").value("Mahindra Bolero Pickup"));
    }

    @Test
    void favoriteToggles() throws Exception {
        long listingId = listingId(1);

        mvc.perform(post("/api/rentals/{id}/favorite", listingId).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listingId))
                .andExpect(jsonPath("$.favorite").value(true));
        mvc.perform(post("/api/rentals/{id}/favorite", listingId).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(false));
    }

    @Test
    void bookingAnUnknownListingIsNotFound() throws Exception {
        mvc.perform(post("/api/rentals/{id}/bookings", 999).with(users.demoFarmer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Rental listing 999 not found"));
    }

    @Test
    void overlongSlotLabelIsRejected() throws Exception {
        String slotLabel = "x".repeat(81);

        mvc.perform(post("/api/rentals/{id}/bookings", listingId(0))
                        .with(users.demoFarmer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotLabel\": \"" + slotLabel + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("slotLabel size must be between 0 and 80"));
    }

    private long listingId(int index) throws Exception {
        return JsonBodies.readId(
                mvc.perform(get("/api/rentals").with(users.demoFarmer())).andReturn(), "$.listings[" + index + "].id");
    }
}
