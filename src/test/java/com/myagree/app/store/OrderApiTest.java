package com.myagree.app.store;

import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.FixedClockConfiguration;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

/** The farmer's store orders. */
@AgriScanApiTest
class OrderApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void ordersListTheirLinesNewestFirst() throws Exception {
        long first = placeOrder("CASH_ON_DELIVERY");
        mvc.perform(post("/api/cart/items").with(users.demoFarmer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\": %d, \"quantity\": 2}".formatted(seedsId()))).andExpect(status().isOk());
        long second = placeOrder("ONLINE");

        mvc.perform(get("/api/orders").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[0].status").value("AWAITING_PAYMENT"))
                .andExpect(jsonPath("$[0].paymentMethod").value("ONLINE"))
                .andExpect(jsonPath("$[1].id").value(first))
                .andExpect(jsonPath("$[1].status").value("PLACED"))
                .andExpect(jsonPath("$[1].placedAt").value(FixedClockConfiguration.NOW.toString()))
                .andExpect(jsonPath("$[1].total").value(730))
                .andExpect(jsonPath("$[1].itemCount").value(2))
                .andExpect(jsonPath("$[1].summary").value("Mancozeb 500g + Doodh Dhara 5kg"))
                .andExpect(jsonPath("$[1].lines.length()").value(2))
                .andExpect(jsonPath("$[1].lines[0].name").value("Indofil M-45 Mancozeb 75% WP"))
                .andExpect(jsonPath("$[1].lines[0].quantity").value(1))
                .andExpect(jsonPath("$[1].lines[0].unitPrice").value(280))
                .andExpect(jsonPath("$[1].lines[0].lineTotal").value(280));
        mvc.perform(get("/api/orders/{id}", first).with(users.demoFarmer()).header(HttpHeaders.ACCEPT_LANGUAGE, "hi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("मैन्कोज़ेब 500 ग्राम + दूध धारा 5 किलो"))
                .andExpect(jsonPath("$.lines[1].name").value("दूध धारा पशु खनिज मिश्रण"));
    }

    @Test
    void farmersOnlySeeTheirOwnOrders() throws Exception {
        long order = placeOrder("CASH_ON_DELIVERY");

        mvc.perform(get("/api/orders").with(users.secondFarmer())).andExpect(jsonPath("$").value(empty()));
        mvc.perform(get("/api/orders/{id}", order).with(users.secondFarmer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order " + order + " not found"));
        mvc.perform(get("/api/orders").with(users.owner())).andExpect(status().isForbidden());
    }

    private long placeOrder(String paymentMethod) throws Exception {
        return JsonBodies.readId(mvc.perform(post("/api/cart/checkout").with(users.demoFarmer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\": \"%s\"}".formatted(paymentMethod)))
                .andExpect(status().isCreated())
                .andReturn(), "$.orderId");
    }

    private long seedsId() throws Exception {
        return JsonBodies.readId(mvc.perform(get("/api/store/products").param("category", "SEEDS")
                .with(users.demoFarmer())).andReturn(), "$[0].id");
    }
}
