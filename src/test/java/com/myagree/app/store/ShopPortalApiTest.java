package com.myagree.app.store;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

/**
 * The shopkeeper portal: the shop's Scan & Pay set-up, its products and its orders, each shop seeing only its own.
 * The demo depot (Sanjay Kulkarni) sells the demo farmer's cart; the Karmala shop (Mahesh Jadhav) sells tools.
 */
@AgriScanApiTest
class ShopPortalApiTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
    private static final String NEW_PRODUCT = """
            {"name": {"en": "Sulphur 80%% WDG", "mr": "सल्फर 80%% WDG", "hi": null},
             "category": "CROP_MEDICINE", "packSize": {"en": "1 kg"},
             "description": {"en": "Powdery mildew control for grapes"},
             "price": %d, "mrp": %s, "inStock": true, "barcode": %s}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void theShopkeeperSeesAndEditsTheShopsScanAndPaySetUp() throws Exception {
        mvc.perform(get("/api/shop/me").with(users.shopkeeper()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Solapur Mandi Agro Depot"))
                .andExpect(jsonPath("$.phone").value(TestUsers.SHOPKEEPER_PHONE))
                .andExpect(jsonPath("$.upiId").value("solapur.agro@agriscandemo"))
                .andExpect(jsonPath("$.acceptsScanAndPay").value(true));

        updateProfile("{\"name\": \"Sanjay Agro Kendra\", \"place\": \"Solapur\", \"upiId\": \"sanjay@okaxis\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sanjay Agro Kendra"))
                .andExpect(jsonPath("$.upiId").value("sanjay@okaxis"));
        updateProfile("{\"name\": \"Sanjay Agro Kendra\", \"place\": \"Solapur\", \"upiId\": \"not a upi id\"}")
                .andExpect(status().isBadRequest());
        updateProfile("{\"name\": \"Sanjay Agro Kendra\", \"place\": \"Solapur\", \"upiId\": \"\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upiId").value(nullValue()))
                .andExpect(jsonPath("$.acceptsScanAndPay").value(false));
    }

    @Test
    void theShopsOwnQrIsServedThroughASignedLinkUntilItIsRemoved() throws Exception {
        MvcResult uploaded = mvc.perform(multipart("/api/shop/me/upi-qr")
                        .file(new MockMultipartFile("image", "phonepe-qr.png", MediaType.IMAGE_PNG_VALUE, PNG))
                        .with(users.shopkeeper()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upiQrUrl").value(startsWith("/api/media/shop-qr/")))
                .andReturn();

        String qrUrl = JsonBodies.read(uploaded, "$.upiQrUrl");
        mvc.perform(get(qrUrl)).andExpect(status().isOk()).andExpect(content().bytes(PNG));

        mvc.perform(delete("/api/shop/me/upi-qr").with(users.shopkeeper()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upiQrUrl").value(nullValue()));
        mvc.perform(get(qrUrl)).andExpect(status().isNotFound());
    }

    @Test
    void aShopListsItsOwnProductsOnly() throws Exception {
        mvc.perform(get("/api/shop/products").with(users.secondShopkeeper()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].category", everyItem(org.hamcrest.Matchers.is("TOOLS"))));
        mvc.perform(get("/api/shop/products").with(users.shopkeeper()))
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[*].category", not(hasItem("TOOLS"))));
    }

    @Test
    void aNewProductAppearsInTheStoreUnderItsShop() throws Exception {
        long id = JsonBodies.readId(createProduct(450, "520", "null")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name.en").value("Sulphur 80% WDG"))
                .andExpect(jsonPath("$.name.mr").value("सल्फर 80% WDG"))
                .andExpect(jsonPath("$.packSize.en").value("1 kg"))
                .andExpect(jsonPath("$.inStock").value(true))
                .andReturn(), "$.id");

        mvc.perform(get("/api/store/products").param("q", "sulphur").with(users.demoFarmer())
                        .header("Accept-Language", "mr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].name").value("सल्फर 80% WDG"))
                .andExpect(jsonPath("$[0].price").value(450))
                .andExpect(jsonPath("$[0].shopName").value("Solapur Mandi Agro Depot"));
    }

    @Test
    void productsAreCheckedBeforeTheyAreListed() throws Exception {
        createProduct(450, "400", "null")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The MRP cannot be lower than the selling price"));
        createProduct(450, "null", "\"1234567890123\"").andExpect(status().isBadRequest());
        createProduct(450, "null", "\"8904567000058\"")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Another product already uses barcode 8904567000058"));
        createProduct(0, "null", "null").andExpect(status().isBadRequest());
    }

    @Test
    void theShopkeeperEditsStocksAndPhotographsTheirProductsButNotOthers() throws Exception {
        long sprayer = firstProductId(users.secondShopkeeper());

        mvc.perform(put("/api/shop/products/{id}", sprayer).with(users.secondShopkeeper())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_PRODUCT.formatted(999, "1200", "null")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(999));
        mvc.perform(patch("/api/shop/products/{id}/stock", sprayer).with(users.secondShopkeeper())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inStock\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inStock").value(false));
        mvc.perform(multipart("/api/shop/products/{id}/photo", sprayer)
                        .file(new MockMultipartFile("image", "sprayer.png", MediaType.IMAGE_PNG_VALUE, PNG))
                        .with(users.secondShopkeeper()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(startsWith("/api/media/product/")));

        mvc.perform(get("/api/shop/products/{id}", sprayer).with(users.shopkeeper())).andExpect(status().isNotFound());
        mvc.perform(patch("/api/shop/products/{id}/stock", sprayer).with(users.shopkeeper())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inStock\": true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void theShopSeesTheOrdersPlacedWithItAndTodaysFigures() throws Exception {
        mvc.perform(post("/api/cart/checkout").with(users.demoFarmer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMethod\": \"CASH_ON_DELIVERY\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/shop/orders").with(users.shopkeeper()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerName").value("Rishikesh"))
                .andExpect(jsonPath("$[0].customerPhone").value(TestUsers.DEMO_FARMER_PHONE))
                .andExpect(jsonPath("$[0].total").value(730))
                .andExpect(jsonPath("$[0].lines", hasSize(2)));
        mvc.perform(get("/api/shop/orders").with(users.secondShopkeeper())).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/shop/dashboard").with(users.shopkeeper()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shopName").value("Solapur Mandi Agro Depot"))
                .andExpect(jsonPath("$.ordersToday").value(1))
                .andExpect(jsonPath("$.salesToday").value(730))
                .andExpect(jsonPath("$.paymentsToVerify").value(0))
                .andExpect(jsonPath("$.products").value(12))
                .andExpect(jsonPath("$.recentOrders", hasSize(1)));
    }

    @Test
    void theShopPortalIsForShopkeepersOnly() throws Exception {
        mvc.perform(get("/api/shop/me").with(users.demoFarmer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/shop/me").with(users.owner())).andExpect(status().isForbidden());
        mvc.perform(get("/api/shop/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cart").with(users.shopkeeper())).andExpect(status().isForbidden());
    }

    private ResultActions updateProfile(String json) throws Exception {
        return mvc.perform(put("/api/shop/me").with(users.shopkeeper())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions createProduct(int price, String mrp, String barcode) throws Exception {
        return mvc.perform(post("/api/shop/products").with(users.shopkeeper())
                .contentType(MediaType.APPLICATION_JSON).content(NEW_PRODUCT.formatted(price, mrp, barcode)));
    }

    private long firstProductId(RequestPostProcessor shopkeeper) throws Exception {
        return JsonBodies.readId(mvc.perform(get("/api/shop/products").with(shopkeeper)).andReturn(), "$[0].id");
    }
}
