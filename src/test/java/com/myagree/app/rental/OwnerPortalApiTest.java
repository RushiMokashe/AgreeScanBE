package com.myagree.app.rental;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

/** The vehicle-owner portal: profile, dashboard, vehicles and the owner's moves of the booking lifecycle. */
@AgriScanApiTest
class OwnerPortalApiTest {

    private static final String ME = "/api/owner/me";
    private static final String LISTINGS = "/api/owner/listings";
    private static final String BOOKINGS = "/api/owner/bookings";
    private static final String SHIVRAJ_PHONE = "9800098765";
    private static final String VINOD_PHONE = "9822233445";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void ownersSeeAndEditTheirProfile() throws Exception {
        mvc.perform(get(ME).with(users.owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rameshwar Patil"))
                .andExpect(jsonPath("$.businessName").value(nullValue()))
                .andExpect(jsonPath("$.phone").value(TestUsers.OWNER_PHONE))
                .andExpect(jsonPath("$.userId").value(users.account(TestUsers.OWNER_PHONE).id()))
                .andExpect(jsonPath("$.hubName").value("Solapur APMC Hub"));
        mvc.perform(get(ME).with(users.owner()).header(HttpHeaders.ACCEPT_LANGUAGE, "mr"))
                .andExpect(jsonPath("$.hubName").value("सोलापूर APMC हब"));

        long puneHubId = hubId(1);
        mvc.perform(put(ME).with(users.owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \" Rameshwar B. Patil \", \"businessName\": \"Patil Krishi Seva\", \"hubId\": %d}"
                                .formatted(puneHubId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rameshwar B. Patil"))
                .andExpect(jsonPath("$.businessName").value("Patil Krishi Seva"))
                .andExpect(jsonPath("$.hubId").value(puneHubId));
        mvc.perform(get("/api/rentals").with(users.demoFarmer()))
                .andExpect(jsonPath("$.listings[0].operator.name").value("Rameshwar B. Patil"))
                .andExpect(jsonPath("$.listings[0].operator.initials").value("RB"));

        mvc.perform(put(ME).with(users.owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \" \", \"hubId\": %d}".formatted(puneHubId)))
                .andExpect(status().isBadRequest());
        mvc.perform(put(ME).with(users.owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Rameshwar\", \"hubId\": 999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("There is no rental hub 999"));
    }

    @Test
    void theDashboardSumsUpTheOwnersVehiclesAndBookings() throws Exception {
        mvc.perform(get("/api/owner/dashboard").with(users.owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingsTotal").value(1))
                .andExpect(jsonPath("$.listingsOnline").value(1))
                .andExpect(jsonPath("$.pendingRequests").value(1))
                .andExpect(jsonPath("$.earningsThisMonth").value(0))
                .andExpect(jsonPath("$.upcoming.length()").value(0));
        mvc.perform(get("/api/owner/dashboard").with(users.signedInAs(VINOD_PHONE)))
                .andExpect(jsonPath("$.pendingRequests").value(0))
                .andExpect(jsonPath("$.upcoming.length()").value(1))
                .andExpect(jsonPath("$.upcoming[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.upcoming[0].farmerName").value("Rishikesh"))
                .andExpect(jsonPath("$.upcoming[0].farmerPhone").value(TestUsers.DEMO_FARMER_PHONE))
                .andExpect(jsonPath("$.upcoming[0].farmerLocation").value("Solapur, Maharashtra"));
    }

    @Test
    void ownersListTheirVehiclesInEveryLanguage() throws Exception {
        MvcResult listings = mvc.perform(get(LISTINGS).with(users.owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name.en").value("Mahindra 575 DI (45 HP) + Rotavator"))
                .andExpect(jsonPath("$[0].name.mr").value("महिंद्रा 575 DI (45 HP) + रोटाव्हेटर"))
                .andExpect(jsonPath("$[0].name.hi").value("महिंद्रा 575 DI (45 HP) + रोटावेटर"))
                .andExpect(jsonPath("$[0].rate").value(650))
                .andExpect(jsonPath("$[0].rateNote.en").value("With Diesel + Driver"))
                .andExpect(jsonPath("$[0].availability.mr").value("स्लॉट: आज दुपारी 02:00"))
                .andExpect(jsonPath("$[0].specs.length()").value(3))
                .andExpect(jsonPath("$[0].specs[0].label.hi").value("रोटावेटर चौड़ाई"))
                .andExpect(jsonPath("$[0].online").value(true))
                .andExpect(jsonPath("$[0].openBookings").value(1))
                .andReturn();

        long rotavator = JsonBodies.readId(listings, "$[0].id");
        mvc.perform(get(LISTINGS + "/{id}", rotavator).with(users.owner())).andExpect(status().isOk());
        mvc.perform(get(LISTINGS + "/{id}", rotavator).with(users.signedInAs(SHIVRAJ_PHONE)))
                .andExpect(status().isNotFound());
        mvc.perform(put(LISTINGS + "/{id}", rotavator).with(users.signedInAs(SHIVRAJ_PHONE))
                        .contentType(MediaType.APPLICATION_JSON).content(listingJson(hubId(0), 500, "[]")))
                .andExpect(status().isNotFound());
    }

    @Test
    void aNewVehicleAppearsForFarmersWithDefaultLabels() throws Exception {
        long solapur = hubId(0);
        MvcResult created = mvc.perform(post(LISTINGS).with(users.owner()).contentType(MediaType.APPLICATION_JSON)
                        .content(listingJson(solapur, 900, "[{\"value\": {\"en\": \"7 Feet\"}, \"label\": {\"en\": \"Width\"}}]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name.en").value("Kubota MU5502 + Rotavator"))
                .andExpect(jsonPath("$.name.mr").value("कुबोटा MU5502 + रोटाव्हेटर"))
                .andExpect(jsonPath("$.name.hi").value(nullValue()))
                .andExpect(jsonPath("$.imageUrl").value(nullValue()))
                .andExpect(jsonPath("$.icon").value(nullValue()))
                .andExpect(jsonPath("$.openBookings").value(0))
                .andReturn();
        long listingId = JsonBodies.readId(created, "$.id");

        mvc.perform(get("/api/rentals").with(users.demoFarmer()).header(HttpHeaders.ACCEPT_LANGUAGE, "hi"))
                .andExpect(jsonPath("$.listings.length()").value(5))
                .andExpect(jsonPath("$.listings[4].id").value(listingId))
                .andExpect(jsonPath("$.listings[4].name").value("Kubota MU5502 + Rotavator"))
                .andExpect(jsonPath("$.listings[4].icon").value("agriculture"))
                .andExpect(jsonPath("$.listings[4].operator.stats").value("AgriScan पर नए"))
                .andExpect(jsonPath("$.listings[4].callLabel").value("Rameshwar को कॉल करें"))
                .andExpect(jsonPath("$.listings[4].bookLabel").value("स्लॉट बुक करें"));
        mvc.perform(get("/api/owner/dashboard").with(users.owner())).andExpect(jsonPath("$.listingsTotal").value(2));
    }

    @Test
    void invalidVehiclesAreRejected() throws Exception {
        long solapur = hubId(0);
        String spec = "{\"value\": {\"en\": \"x\"}, \"label\": {\"en\": \"y\"}}";
        String fourSpecs = "[" + String.join(", ", spec, spec, spec, spec) + "]";
        mvc.perform(createListing(listingJson(solapur, 0, "[]"))).andExpect(status().isBadRequest());
        mvc.perform(createListing(listingJson(solapur, 100_001, "[]"))).andExpect(status().isBadRequest());
        mvc.perform(createListing(listingJson(solapur, 900, fourSpecs))).andExpect(status().isBadRequest());
        mvc.perform(createListing(listingJson(solapur, 900, "[]").replace("Kubota MU5502 + Rotavator", " ")))
                .andExpect(status().isBadRequest());
        mvc.perform(createListing(listingJson(solapur, 900, "[]").replace("\"icon\": null", "\"icon\": \"Bad Icon!\"")))
                .andExpect(status().isBadRequest());
        mvc.perform(createListing(listingJson(999, 900, "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("There is no rental hub 999"));
    }

    @Test
    void editingAVehicleKeepsThePriceOfBookingsAlreadyMade() throws Exception {
        long rotavator = ownListingId();
        mvc.perform(put(LISTINGS + "/{id}", rotavator).with(users.owner()).contentType(MediaType.APPLICATION_JSON)
                        .content(listingJson(hubId(0), 800, "[]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(800))
                .andExpect(jsonPath("$.specs.length()").value(0))
                .andExpect(jsonPath("$.imageUrl").value("/images/rentals/mahindra-575-rotavator.jpg"));

        mvc.perform(get(BOOKINGS).with(users.owner()))
                .andExpect(jsonPath("$[0].amount").value(1950));
        mvc.perform(get("/api/rentals").with(users.demoFarmer()))
                .andExpect(jsonPath("$.listings[0].rate").value(800))
                .andExpect(jsonPath("$.listings[0].badge").value("Verified Field Tested"));
    }

    @Test
    void photosAreServedThroughSignedLinks() throws Exception {
        long rotavator = ownListingId();
        MvcResult uploaded = mvc.perform(multipart(LISTINGS + "/{id}/photo", rotavator)
                        .file(new MockMultipartFile("image", "tractor.png", MediaType.IMAGE_PNG_VALUE, PNG))
                        .with(users.owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(startsWith("/api/media/listing/")))
                .andReturn();

        String photoUrl = JsonBodies.read(uploaded, "$.imageUrl");
        mvc.perform(get(photoUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG));
        mvc.perform(get("/api/rentals").with(users.demoFarmer()))
                .andExpect(jsonPath("$.listings[0].imageUrl").value(startsWith("/api/media/listing/")));
        mvc.perform(multipart(LISTINGS + "/{id}/photo", rotavator)
                        .file(new MockMultipartFile("image", "x.svg", "image/svg+xml", "<svg/>".getBytes()))
                        .with(users.owner()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please upload a photo (JPEG, PNG or WebP)"));
        mvc.perform(multipart(LISTINGS + "/{id}/photo", rotavator)
                        .file(new MockMultipartFile("image", "empty.png", MediaType.IMAGE_PNG_VALUE, new byte[0]))
                        .with(users.owner()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aVehicleWithOpenBookingsCannotBeRemoved() throws Exception {
        long rotavator = ownListingId();
        mvc.perform(delete(LISTINGS + "/{id}", rotavator).with(users.owner()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("This vehicle has requested or accepted bookings. Finish or decline them first."));

        mvc.perform(post(BOOKINGS + "/{id}/decline", requestedBookingId()).with(users.owner())).andExpect(status().isOk());
        mvc.perform(delete(LISTINGS + "/{id}", rotavator).with(users.owner())).andExpect(status().isNoContent());
        mvc.perform(get(LISTINGS).with(users.owner())).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/rentals/bookings").with(users.demoFarmer()))
                .andExpect(jsonPath("$[2].listingName").value("Mahindra 575 DI (45 HP) + Rotavator"));
    }

    @Test
    void theOwnerTakesAJobFromRequestToCompletionAndTheFarmerHearsOfEveryStep() throws Exception {
        long booking = requestedBookingId();
        mvc.perform(get(BOOKINGS + "/{id}", booking).with(users.owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farmerName").value("Rishikesh"))
                .andExpect(jsonPath("$.canAccept").value(true))
                .andExpect(jsonPath("$.canDecline").value(true))
                .andExpect(jsonPath("$.canStart").value(false))
                .andExpect(jsonPath("$.canComplete").value(false));

        mvc.perform(post(BOOKINGS + "/{id}/accept", booking).with(users.owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.timeline.length()").value(2))
                .andExpect(jsonPath("$.timeline[1].actor").value("OWNER"))
                .andExpect(jsonPath("$.canStart").value(true));
        mvc.perform(get("/api/notifications").with(users.demoFarmer()))
                .andExpect(jsonPath("$.items[0].type").value("BOOKING_ACCEPTED"))
                .andExpect(jsonPath("$.items[0].title").value("Booking accepted"))
                .andExpect(jsonPath("$.items[0].route").value("/bookings/" + booking));
        mvc.perform(post(BOOKINGS + "/{id}/accept", booking).with(users.owner()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only a requested booking can be accepted"));

        mvc.perform(post(BOOKINGS + "/{id}/start", booking).with(users.owner()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.canComplete").value(true));
        mvc.perform(post(BOOKINGS + "/{id}/complete", booking).with(users.owner()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.timeline.length()").value(4));
        mvc.perform(get("/api/notifications").with(users.demoFarmer()).header(HttpHeaders.ACCEPT_LANGUAGE, "mr"))
                .andExpect(jsonPath("$.items[0].type").value("BOOKING_COMPLETED"))
                .andExpect(jsonPath("$.items[0].title").value("काम पूर्ण झाले"))
                .andExpect(jsonPath("$.items[0].body").value(containsString("₹1,950")))
                .andExpect(jsonPath("$.items[1].type").value("BOOKING_STARTED"));
        mvc.perform(post(BOOKINGS + "/{id}/decline", booking).with(users.owner())).andExpect(status().isConflict());
    }

    @Test
    void decliningTellsTheFarmerWhy() throws Exception {
        long booking = requestedBookingId();
        mvc.perform(post(BOOKINGS + "/{id}/decline", booking).with(users.owner())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"Tractor under repair\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.timeline[1].note").value("Tractor under repair"));

        mvc.perform(get("/api/notifications").with(users.demoFarmer()))
                .andExpect(jsonPath("$.items[0].type").value("BOOKING_DECLINED"))
                .andExpect(jsonPath("$.items[0].body")
                        .value("Rameshwar Patil declined your booking of Mahindra 575 DI (45 HP) + Rotavator: Tractor under repair"));
        mvc.perform(get("/api/rentals/bookings/{id}", booking).with(users.demoFarmer()))
                .andExpect(jsonPath("$.message")
                        .value("Rameshwar Patil could not take this booking. Try another vehicle nearby."));
    }

    @Test
    void aNewRequestReachesTheOwner() throws Exception {
        long tataAce = JsonBodies.readId(mvc.perform(get("/api/rentals").with(users.demoFarmer())).andReturn(),
                "$.listings[2].id");
        long booking = JsonBodies.readId(mvc.perform(post("/api/rentals/{id}/bookings", tataAce).with(users.demoFarmer()))
                .andExpect(status().isCreated()).andReturn(), "$.id");

        RequestPostProcessor vinod = users.signedInAs(VINOD_PHONE);
        mvc.perform(get("/api/notifications").with(vinod))
                .andExpect(jsonPath("$.items[0].type").value("BOOKING_REQUESTED"))
                .andExpect(jsonPath("$.items[0].route").value("/owner/bookings/" + booking))
                .andExpect(jsonPath("$.items[0].body")
                        .value("Rishikesh wants Tata Ace Gold (छोटा हाथी) for At Farm in 20m (₹270)"));
        mvc.perform(get(BOOKINGS).param("status", "REQUESTED").with(vinod))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(booking));
    }

    @Test
    void ownersOnlyReachTheirOwnBookings() throws Exception {
        long booking = requestedBookingId();
        RequestPostProcessor shivraj = users.signedInAs(SHIVRAJ_PHONE);
        mvc.perform(get(BOOKINGS + "/{id}", booking).with(shivraj)).andExpect(status().isNotFound());
        mvc.perform(post(BOOKINGS + "/{id}/accept", booking).with(shivraj)).andExpect(status().isNotFound());
        mvc.perform(get(BOOKINGS).with(shivraj)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(BOOKINGS).param("status", "COMPLETED").with(users.owner()))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(BOOKINGS).param("status", "LOST").with(users.owner())).andExpect(status().isBadRequest());
    }

    @Test
    void onlyVehicleOwnersUseThePortal() throws Exception {
        mvc.perform(get(ME).with(users.demoFarmer())).andExpect(status().isForbidden());
        mvc.perform(get(LISTINGS).with(users.admin())).andExpect(status().isForbidden());
        mvc.perform(get(BOOKINGS)).andExpect(status().isUnauthorized());
        mvc.perform(patch(LISTINGS + "/{id}/online", ownListingId()).with(users.owner())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder createListing(String json) {
        return post(LISTINGS).with(users.owner()).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private static String listingJson(long hubId, int rate, String specs) {
        return """
                {"category": "MACHINERY", "hubId": %d,
                 "name": {"en": "Kubota MU5502 + Rotavator", "mr": "कुबोटा MU5502 + रोटाव्हेटर", "hi": null},
                 "description": {"en": "Puddling and rotavation for paddy"},
                 "icon": null, "rate": %d, "rateUnit": "HOUR", "rateNote": null,
                 "availability": {"en": "Slot: 08:00 AM Tomorrow"}, "online": true,
                 "specs": %s, "features": []}""".formatted(hubId, rate, specs);
    }

    private long hubId(int index) throws Exception {
        return JsonBodies.readId(mvc.perform(get("/api/rentals/hubs").with(users.demoFarmer())).andReturn(),
                "$[" + index + "].id");
    }

    private long ownListingId() throws Exception {
        return JsonBodies.readId(mvc.perform(get(LISTINGS).with(users.owner())).andReturn(), "$[0].id");
    }

    private long requestedBookingId() throws Exception {
        return JsonBodies.readId(mvc.perform(get(BOOKINGS).param("status", "REQUESTED").with(users.owner())).andReturn(),
                "$[0].id");
    }
}
