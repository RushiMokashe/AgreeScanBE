package com.myagree.app.rental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.FixedClockConfiguration;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

/** The farmer's side of rentals: hubs, listings, booking, following and cancelling bookings, favourites. */
@AgriScanApiTest
class RentalApiTest {

    private static final String RENTALS = "/api/rentals";
    private static final String MY_BOOKINGS = "/api/rentals/bookings";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void theSolapurHubLooksAsDesigned() throws Exception {
        MvcResult overview = mvc.perform(get(RENTALS).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hubName").value("Solapur APMC Hub"))
                .andExpect(jsonPath("$.radiusKm").value(12))
                .andExpect(jsonPath("$.routes").value("Karmala, Kem & Kurduwadi routes"))
                .andExpect(jsonPath("$.onlineCount").value(5))
                .andExpect(jsonPath("$.spotlight.title").value("Mahindra Bolero Pickup"))
                .andExpect(jsonPath("$.spotlight.subtitle").value("Rated 1.7 Tonne • 65–75 Vegetable Crates"))
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
    void cardsShowTheOwnerAsOperatorAndPhotosOrIcons() throws Exception {
        mvc.perform(get(RENTALS).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listings[0].category").value("MACHINERY"))
                .andExpect(jsonPath("$.listings[0].name").value("Mahindra 575 DI (45 HP) + Rotavator"))
                .andExpect(jsonPath("$.listings[0].imageUrl").value("/images/rentals/mahindra-575-rotavator.jpg"))
                .andExpect(jsonPath("$.listings[0].icon").value(nullValue()))
                .andExpect(jsonPath("$.listings[0].badge").value("Verified Field Tested"))
                .andExpect(jsonPath("$.listings[0].distanceKm").value(2.4))
                .andExpect(jsonPath("$.listings[0].locality").value("Kem Shivar"))
                .andExpect(jsonPath("$.listings[0].rateUnit").value("HOUR"))
                .andExpect(jsonPath("$.listings[0].operator.name").value("Rameshwar Patil"))
                .andExpect(jsonPath("$.listings[0].operator.initials").value("RP"))
                .andExpect(jsonPath("$.listings[0].operator.stats").value("120+ acres tilled • 4.9 ★★★★★"))
                .andExpect(jsonPath("$.listings[0].phone").value("+919800012345"))
                .andExpect(jsonPath("$.listings[0].callLabel").value("Call Rameshwar"))
                .andExpect(jsonPath("$.listings[0].specs.length()").value(3))
                .andExpect(jsonPath("$.listings[0].specs[0].value").value("6 Feet"))
                .andExpect(jsonPath("$.listings[0].favorite").value(false))
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
    void everythingIsShownInTheReadersLanguage() throws Exception {
        mvc.perform(get(RENTALS).with(users.demoFarmer()).header(HttpHeaders.ACCEPT_LANGUAGE, "mr"))
                .andExpect(jsonPath("$.hubName").value("सोलापूर APMC हब"))
                .andExpect(jsonPath("$.spotlight.perks[0].title").value("शून्य सांडणी"))
                .andExpect(jsonPath("$.listings[0].name").value("महिंद्रा 575 DI (45 HP) + रोटाव्हेटर"))
                .andExpect(jsonPath("$.listings[0].operator.name").value("Rameshwar Patil"))
                .andExpect(jsonPath("$.listings[0].callLabel").value("रामेश्वर यांना कॉल करा"));
        mvc.perform(get(RENTALS).with(users.demoFarmer()).header(HttpHeaders.ACCEPT_LANGUAGE, "hi"))
                .andExpect(jsonPath("$.routes").value("करमाला, केम और कुर्डुवाडी मार्ग"))
                .andExpect(jsonPath("$.listings[0].availability").value("स्लॉट: आज दोपहर 02:00"))
                .andExpect(jsonPath("$.listings[2].features[0].text").value("सोलापुर APMC सीधा प्रवेश पास"));
    }

    @Test
    void everyHubShowsItsOwnVehicles() throws Exception {
        MvcResult hubs = mvc.perform(get(RENTALS + "/hubs").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].name").value("Pune Market Yard Hub"))
                .andReturn();

        mvc.perform(get(RENTALS).param("hubId", String.valueOf(JsonBodies.readId(hubs, "$[1].id"))).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hubName").value("Pune Market Yard Hub"))
                .andExpect(jsonPath("$.spotlight").value(nullValue()))
                .andExpect(jsonPath("$.onlineCount").value(2))
                .andExpect(jsonPath("$.listings[0].imageUrl").value(nullValue()))
                .andExpect(jsonPath("$.listings[0].icon").value("agriculture"))
                .andExpect(jsonPath("$.listings[0].callLabel").value("Call Ganesh"))
                .andExpect(jsonPath("$.listings[0].bookLabel").value("Book Slot"))
                .andExpect(jsonPath("$.listings[0].bookIcon").value("calendar_month"))
                .andExpect(jsonPath("$.listings[1].bookLabel").value("Book Transport"))
                .andExpect(jsonPath("$.listings[1].operator.name").value("Pawar Transport"));
        mvc.perform(get(RENTALS).param("hubId", String.valueOf(JsonBodies.readId(hubs, "$[2].id"))).with(users.demoFarmer()))
                .andExpect(jsonPath("$.listings.length()").value(2))
                .andExpect(jsonPath("$.listings[?(@.name =~ /.*Day Hire.*/)]").isEmpty());
        mvc.perform(get(RENTALS).param("hubId", "999").with(users.demoFarmer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Rental hub 999 not found"));
    }

    @Test
    void bookingAsksTheOwnerAndPricesTheDefaultEstimate() throws Exception {
        mvc.perform(book(listingId(0)).content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.listingName").value("Mahindra 575 DI (45 HP) + Rotavator"))
                .andExpect(jsonPath("$.operatorName").value("Rameshwar Patil"))
                .andExpect(jsonPath("$.operatorPhone").value("9800012345"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.slotLabel").value("Slot: 02:00 PM Today"))
                .andExpect(jsonPath("$.estimatedUnits").value(2))
                .andExpect(jsonPath("$.rateUnit").value("HOUR"))
                .andExpect(jsonPath("$.amount").value(1300))
                .andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.createdAt").value(FixedClockConfiguration.NOW.toString()))
                .andExpect(jsonPath("$.message")
                        .value("Request sent to Rameshwar Patil. We will notify you when they accept."));
    }

    @Test
    void bookingKeepsTheRequestedSlotAndEstimate() throws Exception {
        mvc.perform(book(listingId(2)).content("{\"slotLabel\": \"Tomorrow 5:00 AM\", \"estimatedUnits\": 30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slotLabel").value("Tomorrow 5:00 AM"))
                .andExpect(jsonPath("$.rateUnit").value("KM"))
                .andExpect(jsonPath("$.amount").value(540));
    }

    @Test
    void theSpotlightTripAddsItsBaseFare() throws Exception {
        long spotlightListingId = JsonBodies.readId(mvc.perform(get(RENTALS).with(users.demoFarmer())).andReturn(),
                "$.spotlight.listingId");

        mvc.perform(post(RENTALS + "/{id}/bookings", spotlightListingId).with(users.demoFarmer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingName").value("Mahindra Bolero Pickup"))
                .andExpect(jsonPath("$.estimatedUnits").value(15))
                .andExpect(jsonPath("$.amount").value(450 + 22 * 15));
    }

    @Test
    void aBookingWithoutASlotTakesTheNextOneInTheFarmersLanguage() throws Exception {
        mvc.perform(book(listingId(0)).content("{}").header(HttpHeaders.ACCEPT_LANGUAGE, "mr"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slotLabel").value("स्लॉट: आज दुपारी 02:00"))
                .andExpect(jsonPath("$.message").value(containsString("Rameshwar Patil यांना विनंती पाठवली")));
    }

    @Test
    void invalidBookingsAreRejected() throws Exception {
        long listingId = listingId(0);
        mvc.perform(book(listingId).content("{\"slotLabel\": \"" + "x".repeat(81) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("slotLabel size must be between 0 and 80"));
        mvc.perform(book(listingId).content("{\"estimatedUnits\": 0}")).andExpect(status().isBadRequest());
        mvc.perform(book(listingId).content("{\"estimatedUnits\": 501}")).andExpect(status().isBadRequest());
        mvc.perform(post(RENTALS + "/{id}/bookings", 999).with(users.demoFarmer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Rental listing 999 not found"));
    }

    @Test
    void aVehicleTakenOfflineCannotBeBooked() throws Exception {
        long listingId = listingId(0);
        mvc.perform(patch("/api/owner/listings/{id}/online", listingId)
                        .with(users.owner()).contentType(MediaType.APPLICATION_JSON).content("{\"online\": false}"))
                .andExpect(status().isOk());

        mvc.perform(book(listingId).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This vehicle is not taking bookings right now"));
        mvc.perform(get(RENTALS).with(users.demoFarmer()))
                .andExpect(jsonPath("$.listings.length()").value(3))
                .andExpect(jsonPath("$.onlineCount").value(4));
    }

    @Test
    void farmersSeeOnlyTheirOwnBookingsNewestFirst() throws Exception {
        MvcResult mine = mvc.perform(get(MY_BOOKINGS).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$[0].amount").value(1950))
                .andExpect(jsonPath("$[1].status").value("ACCEPTED"))
                .andExpect(jsonPath("$[1].listingName").value("Tata Ace Gold (छोटा हाथी)"))
                .andExpect(jsonPath("$[1].amount").value(360))
                .andExpect(jsonPath("$[1].message")
                        .value("Vinod Shinde accepted your booking and will come at the booked time."))
                .andExpect(jsonPath("$[2].status").value("COMPLETED"))
                .andReturn();

        mvc.perform(get(MY_BOOKINGS).with(users.secondFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(empty()));
        long rishikeshBooking = JsonBodies.readId(mine, "$[0].id");
        mvc.perform(get(MY_BOOKINGS + "/{id}", rishikeshBooking).with(users.secondFarmer()))
                .andExpect(status().isNotFound());
        mvc.perform(post(MY_BOOKINGS + "/{id}/cancel", rishikeshBooking).with(users.secondFarmer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void theTrackingScreenShowsTheTimelineAndWhatTheFarmerCanDo() throws Exception {
        long accepted = myBookingId(1);
        mvc.perform(get(MY_BOOKINGS + "/{id}", accepted).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.hubName").value("Solapur APMC Hub"))
                .andExpect(jsonPath("$.listingImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.listingIcon").value("local_shipping"))
                .andExpect(jsonPath("$.timeline.length()").value(2))
                .andExpect(jsonPath("$.timeline[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$.timeline[0].actor").value("FARMER"))
                .andExpect(jsonPath("$.timeline[1].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.timeline[1].actor").value("OWNER"))
                .andExpect(jsonPath("$.canCancel").value(true))
                .andExpect(jsonPath("$.canPay").value(true))
                .andExpect(jsonPath("$.paymentId").value(nullValue()));

        mvc.perform(get(MY_BOOKINGS + "/{id}", myBookingId(2)).with(users.demoFarmer()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.timeline.length()").value(4))
                .andExpect(jsonPath("$.canCancel").value(false))
                .andExpect(jsonPath("$.canPay").value(true));
        mvc.perform(get(MY_BOOKINGS + "/{id}", myBookingId(0)).with(users.demoFarmer()))
                .andExpect(jsonPath("$.canPay").value(false));
    }

    @Test
    void cancellingRecordsTheReasonAndTellsTheOwner() throws Exception {
        long requested = myBookingId(0);
        mvc.perform(post(MY_BOOKINGS + "/{id}/cancel", requested).with(users.demoFarmer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"Rain expected tomorrow\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.message").value("You cancelled this booking."))
                .andExpect(jsonPath("$.timeline[1].status").value("CANCELLED"))
                .andExpect(jsonPath("$.timeline[1].actor").value("FARMER"))
                .andExpect(jsonPath("$.timeline[1].note").value("Rain expected tomorrow"))
                .andExpect(jsonPath("$.canCancel").value(false));

        mvc.perform(get("/api/notifications").with(users.owner()))
                .andExpect(jsonPath("$.items[0].type").value("BOOKING_CANCELLED"))
                .andExpect(jsonPath("$.items[0].route").value("/owner/bookings/" + requested))
                .andExpect(jsonPath("$.items[0].body").value(containsString("Rain expected tomorrow")));
        mvc.perform(post(MY_BOOKINGS + "/{id}/cancel", requested).with(users.demoFarmer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This booking can no longer be cancelled"));
        mvc.perform(post(MY_BOOKINGS + "/{id}/cancel", myBookingId(2)).with(users.demoFarmer()))
                .andExpect(status().isConflict());
        mvc.perform(post(MY_BOOKINGS + "/{id}/cancel", myBookingId(1)).with(users.demoFarmer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"" + "x".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void favoritesBelongToEachFarmer() throws Exception {
        long listingId = listingId(1);

        mvc.perform(post(RENTALS + "/{id}/favorite", listingId).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listingId))
                .andExpect(jsonPath("$.favorite").value(true));
        mvc.perform(get(RENTALS).with(users.demoFarmer())).andExpect(jsonPath("$.listings[1].favorite").value(true));
        mvc.perform(get(RENTALS).with(users.secondFarmer())).andExpect(jsonPath("$.listings[1].favorite").value(false));
        mvc.perform(post(RENTALS + "/{id}/favorite", listingId).with(users.demoFarmer()))
                .andExpect(jsonPath("$.favorite").value(false));
    }

    @Test
    void onlyFarmersUseTheFarmerRentalsButEveryoneSeesTheHubs() throws Exception {
        mvc.perform(get(RENTALS + "/hubs").with(users.owner())).andExpect(status().isOk());
        mvc.perform(get(RENTALS + "/hubs").with(users.admin())).andExpect(status().isOk());
        mvc.perform(get(RENTALS + "/hubs")).andExpect(status().isUnauthorized());
        mvc.perform(get(RENTALS).with(users.owner())).andExpect(status().isForbidden());
        mvc.perform(get(MY_BOOKINGS).with(users.admin())).andExpect(status().isForbidden());
        mvc.perform(get(RENTALS)).andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder book(long listingId) {
        return post(RENTALS + "/{id}/bookings", listingId).with(users.demoFarmer()).contentType(MediaType.APPLICATION_JSON);
    }

    private long listingId(int index) throws Exception {
        return JsonBodies.readId(mvc.perform(get(RENTALS).with(users.demoFarmer())).andReturn(),
                "$.listings[" + index + "].id");
    }

    private long myBookingId(int index) throws Exception {
        return JsonBodies.readId(mvc.perform(get(MY_BOOKINGS).with(users.demoFarmer())).andReturn(), "$[" + index + "].id");
    }
}
