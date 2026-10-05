package com.myagree.app.mandi;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

@AgriScanApiTest
class MandiApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void overviewShowsMarketForecastAndCommodityRates() throws Exception {
        mvc.perform(get("/api/mandi").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.market.name").value("Solapur APMC Mandi"))
                .andExpect(jsonPath("$.market.shortName").value("Solapur Main APMC"))
                .andExpect(jsonPath("$.market.open").value(true))
                .andExpect(jsonPath("$.market.boardLabel").value("Live Wholesale Board • आजचे बाजारभाव"))
                .andExpect(jsonPath("$.market.arrivalsQuintals").value(8450))
                .andExpect(jsonPath("$.market.helplineDisplay").value("1800-233-1020"))
                .andExpect(jsonPath("$.market.helplineHours").value("6 AM - 8 PM"))
                .andExpect(jsonPath("$.forecast.tag").value("Festive Surge"))
                .andExpect(jsonPath("$.pricesUpdatedAt").value("2026-09-29T04:12:00Z"))
                .andExpect(jsonPath("$.commodities.length()").value(4))
                .andExpect(jsonPath("$.commodities[0].name").value("Tomato (Hybrid)"))
                .andExpect(jsonPath("$.commodities[0].minPrice").value(1850))
                .andExpect(jsonPath("$.commodities[0].maxPrice").value(2400))
                .andExpect(jsonPath("$.commodities[0].unit").value("Quintal"))
                .andExpect(jsonPath("$.commodities[0].changeAmount").value(180))
                .andExpect(jsonPath("$.commodities[0].demand").value("HIGH_DEMAND"))
                .andExpect(jsonPath("$.commodities[1].changeAmount").value(0))
                .andExpect(jsonPath("$.commodities[3].demand").value("HOT"));
    }

    @Test
    void overviewListsTheThreeBuyerInquiries() throws Exception {
        mvc.perform(get("/api/mandi").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inquiries.length()").value(3))
                .andExpect(jsonPath("$.inquiries[0].buyerName").value("Reliance Retail & BigBasket"))
                .andExpect(jsonPath("$.inquiries[0].verified").value(true))
                .andExpect(jsonPath("$.inquiries[0].pricePerQuintal").value(2450))
                .andExpect(jsonPath("$.inquiries[0].priceNoteHighlighted").value(false))
                .andExpect(jsonPath("$.inquiries[0].specs.length()").value(3))
                .andExpect(jsonPath("$.inquiries[0].actionPrimary").value(true))
                .andExpect(jsonPath("$.inquiries[0].responded").value(false))
                .andExpect(jsonPath("$.inquiries[1].badge").value("+₹200 Premium"))
                .andExpect(jsonPath("$.inquiries[1].priceNote").value("Bonus Grade Incentive"))
                .andExpect(jsonPath("$.inquiries[1].priceNoteHighlighted").value(true))
                .andExpect(jsonPath("$.inquiries[1].actionIcon").value("upload_file"))
                .andExpect(jsonPath("$.inquiries[2].verified").value(false))
                .andExpect(jsonPath("$.inquiries[2].badgeHighlighted").value(false))
                .andExpect(jsonPath("$.inquiries[2].priceNoteHighlighted").value(false))
                .andExpect(jsonPath("$.inquiries[2].highlighted").value(false))
                .andExpect(jsonPath("$.inquiries[2].specsTitle").value(nullValue()))
                .andExpect(jsonPath("$.inquiries[2].specs").value(empty()))
                .andExpect(jsonPath("$.inquiries[2].actionIcon").value(nullValue()));
    }

    @Test
    void respondingToAnInquiryMarksItRespondedAndIsIdempotent() throws Exception {
        long inquiryId = JsonBodies.readId(
                mvc.perform(get("/api/mandi").with(users.demoFarmer())).andReturn(), "$.inquiries[0].id");

        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/api/mandi/inquiries/{id}/respond", inquiryId).with(users.demoFarmer()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(inquiryId))
                    .andExpect(jsonPath("$.responded").value(true))
                    .andExpect(jsonPath("$.specs.length()").value(3));
        }
        mvc.perform(get("/api/mandi").with(users.demoFarmer()))
                .andExpect(jsonPath("$.inquiries[0].responded").value(true));
    }

    @Test
    void respondingToAnUnknownInquiryIsNotFound() throws Exception {
        mvc.perform(post("/api/mandi/inquiries/{id}/respond", 999).with(users.demoFarmer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Buyer inquiry 999 not found"));
    }
}
