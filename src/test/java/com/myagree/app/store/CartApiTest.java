package com.myagree.app.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

@AgriScanApiTest
class CartApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void seededCartMatchesTheDesignsCartBar() throws Exception {
        mvc.perform(get("/api/cart").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].shortName").value("Mancozeb 500g"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(280))
                .andExpect(jsonPath("$.items[0].lineTotal").value(280))
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.total").value(730))
                .andExpect(jsonPath("$.freeDelivery").value(true))
                .andExpect(jsonPath("$.summary").value("Mancozeb 500g + Doodh Dhara 5kg"));
    }

    @Test
    void addingAProductAlreadyInTheCartMergesIntoItsLine() throws Exception {
        long mancozebId = JsonBodies.readId(cart(), "$.items[0].productId");

        addToCart(mancozebId, 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].lineTotal").value(840))
                .andExpect(jsonPath("$.itemCount").value(4))
                .andExpect(jsonPath("$.total").value(1290));
    }

    @Test
    void addingANewProductAppendsALine() throws Exception {
        long seedsId = productId("SEEDS");

        addToCart(seedsId, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[2].id").isNumber())
                .andExpect(jsonPath("$.items[2].productId").value(seedsId))
                .andExpect(jsonPath("$.total").value(1210))
                .andExpect(jsonPath("$.summary").value("Mancozeb 500g + Doodh Dhara 5kg + 1 more"));
    }

    @Test
    void removingALineUpdatesTotals() throws Exception {
        long mancozebLineId = JsonBodies.readId(cart(), "$.items[0].id");

        mvc.perform(delete("/api/cart/items/{itemId}", mancozebLineId).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").value(450))
                .andExpect(jsonPath("$.freeDelivery").value(false))
                .andExpect(jsonPath("$.summary").value("Doodh Dhara 5kg"));
    }

    @Test
    void checkoutPlacesAnOrderAndEmptiesTheCart() throws Exception {
        MvcResult confirmation = mvc.perform(checkout())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.total").value(730))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andReturn();

        long orderId = JsonBodies.readId(confirmation, "$.orderId");
        assertThat(JsonBodies.<String>read(confirmation, "$.message"))
                .isEqualTo("Order #" + orderId + " placed • ₹730 • Cash on Delivery");
        mvc.perform(get("/api/cart").with(users.demoFarmer()))
                .andExpect(jsonPath("$.items").value(empty()))
                .andExpect(jsonPath("$.itemCount").value(0))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.freeDelivery").value(false))
                .andExpect(jsonPath("$.summary").value(""));
    }

    @Test
    void orderMessageUsesIndianDigitGrouping() throws Exception {
        addToCart(productId("TOOLS"), 50, true).andExpect(status().isOk());

        mvc.perform(checkout())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(107500))
                .andExpect(jsonPath("$.message", endsWith(" placed • ₹1,07,500 • Cash on Delivery")));
    }

    /** The demo cart holds the depot's products; tools come from the Karmala shop, which a farmer pays separately. */
    @Test
    void aCartHoldsOneShopsProductsUnlessTheFarmerReplacesThem() throws Exception {
        mvc.perform(get("/api/cart").with(users.demoFarmer()))
                .andExpect(jsonPath("$.shopName").value("Solapur Mandi Agro Depot"));

        addToCart(productId("TOOLS"), 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Your cart has products from Solapur Mandi Agro Depot. A cart holds one shop's products, so you pay "
                                + "one shop at a time. Empty it to buy from Karmala Krishi Seva Kendra?"))
                .andExpect(jsonPath("$.code").value("store.cart.other-shop"));

        long sprayer = productId("TOOLS");
        addToCart(sprayer, 1, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(sprayer))
                .andExpect(jsonPath("$.shopName").value("Karmala Krishi Seva Kendra"));
    }

    @Test
    void anOnlineOrderWaitsForItsPayment() throws Exception {
        mvc.perform(post("/api/cart/checkout")
                        .with(users.demoFarmer())
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "mr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\": \"ONLINE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT"))
                .andExpect(jsonPath("$.paymentMethod").value("ONLINE"))
                .andExpect(jsonPath("$.message", endsWith(" नोंदवली • ₹730 • ऑनलाइन पेमेंट")));
    }

    @Test
    void soldOutProductsCannotBeBought() throws Exception {
        long secateurs = JsonBodies.readId(mvc.perform(get("/api/store/products/barcode/8904567000140")
                .with(users.demoFarmer())).andReturn(), "$.id");
        addToCart(secateurs, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Secateurs is out of stock right now"));

        long mancozeb = JsonBodies.readId(cart(), "$.items[0].productId");
        mvc.perform(patch("/api/admin/products/{id}", mancozeb).with(users.admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inStock\": false}"))
                .andExpect(status().isOk());
        mvc.perform(checkout())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("These products ran out since you added them: Mancozeb 500g. Remove them to check out."));
    }

    @Test
    void checkoutNeedsAPaymentMethod() throws Exception {
        mvc.perform(post("/api/cart/checkout").with(users.demoFarmer())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkoutOfAnEmptyCartIsRejected() throws Exception {
        mvc.perform(checkout()).andExpect(status().isCreated());

        mvc.perform(checkout())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Your cart is empty"));
    }

    @Test
    void invalidCartRequestsAreRejected() throws Exception {
        addToCart(999, 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product 999 not found"));
        addToCart(productId("SEEDS"), 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("quantity must be greater than 0"));
        mvc.perform(post("/api/cart/items")
                        .with(users.demoFarmer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("productId must not be null"));
        mvc.perform(delete("/api/cart/items/{itemId}", 999).with(users.demoFarmer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cart item 999 not found"));
    }

    private MvcResult cart() throws Exception {
        return mvc.perform(get("/api/cart").with(users.demoFarmer())).andExpect(status().isOk()).andReturn();
    }

    private long productId(String category) throws Exception {
        MvcResult products = mvc.perform(get("/api/store/products").param("category", category).with(users.demoFarmer()))
                .andReturn();
        return JsonBodies.readId(products, "$[0].id");
    }

    /** Checks the cart out for cash on delivery. */
    private MockHttpServletRequestBuilder checkout() {
        return post("/api/cart/checkout")
                .with(users.demoFarmer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentMethod\": \"CASH_ON_DELIVERY\"}");
    }

    private ResultActions addToCart(long productId, int quantity) throws Exception {
        return addToCart(productId, quantity, false);
    }

    private ResultActions addToCart(long productId, int quantity, boolean replaceCart) throws Exception {
        return mvc.perform(post("/api/cart/items")
                .with(users.demoFarmer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\": %d, \"quantity\": %d, \"replaceCart\": %s}".formatted(productId, quantity,
                        replaceCart)));
    }
}
