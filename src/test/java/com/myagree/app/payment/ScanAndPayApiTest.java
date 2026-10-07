package com.myagree.app.payment;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

/**
 * Scan & Pay: the farmer pays the shop or the vehicle owner straight to their UPI ID or QR, quotes the UPI reference,
 * and the seller confirms whether the money arrived. The demo depot and the demo farmer's accepted booking (Vinod
 * Shinde's Tata Ace) both have Scan & Pay set up.
 */
@AgriScanApiTest
class ScanAndPayApiTest {

    private static final String PAYMENTS = "/api/payments";
    private static final String SELLER_PAYMENTS = "/api/seller/payments";
    private static final String DEPOT = "Solapur Mandi Agro Depot";
    private static final String UTR = "412345678901";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theCheckoutOffersScanAndPayWithAQrThatFillsInTheAmount() throws Exception {
        long order = checkoutOnline(users.demoFarmer());

        createPayment(users.demoFarmer(), "STORE_ORDER", order)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scanAndPay.payeeName").value(DEPOT))
                .andExpect(jsonPath("$.scanAndPay.upiId").value("solapur.agro@agriscandemo"))
                .andExpect(jsonPath("$.scanAndPay.upiLink").value(
                        ("upi://pay?pa=solapur.agro@agriscandemo&pn=Solapur%20Mandi%20Agro%20Depot&am=730.00&cu=INR"
                                + "&tn=Agro%20Store%20order%20%23" + order)))
                .andExpect(jsonPath("$.scanAndPay.generatedQr").value(startsWith("data:image/png;base64,iVBORw0KGgo")))
                .andExpect(jsonPath("$.scanAndPay.qrImageUrl").value(nullValue()));
    }

    @Test
    void aShopWithoutScanAndPayOffersNone() throws Exception {
        mvc.perform(put("/api/shop/me").with(users.shopkeeper()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\", \"place\": \"Solapur\", \"upiId\": \"\"}".formatted(DEPOT)))
                .andExpect(status().isOk());
        long order = checkoutOnline(users.demoFarmer());

        createPayment(users.demoFarmer(), "STORE_ORDER", order).andExpect(jsonPath("$.scanAndPay").value(nullValue()));
        scanAndPay(users.demoFarmer(), "STORE_ORDER", order, UTR)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "This seller has not set up Scan & Pay yet. Please pay by card or UPI."));
    }

    @Test
    void theShopkeeperConfirmsAPaymentTheFarmerSentThemDirectly() throws Exception {
        long order = checkoutOnline(users.demoFarmer());
        createPayment(users.demoFarmer(), "STORE_ORDER", order).andExpect(status().isCreated());

        long paymentId = JsonBodies.readId(scanAndPay(users.demoFarmer(), "STORE_ORDER", order, UTR)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("SCAN_AND_PAY"))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.payeeName").value(DEPOT))
                .andExpect(jsonPath("$.upiReference").value(UTR))
                .andReturn(), "$.paymentId");

        // Only the shopkeeper decides; the farmer can neither confirm it nor start paying again meanwhile
        mvc.perform(post(PAYMENTS + "/{id}/confirm", paymentId).with(users.demoFarmer()))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
        createPayment(users.demoFarmer(), "STORE_ORDER", order)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(("Your Scan & Pay payment is waiting for %s to confirm it. You can "
                        + "pay again if they say it did not arrive.").formatted(DEPOT)));

        mvc.perform(get(SELLER_PAYMENTS).param("status", "PROCESSING").with(users.shopkeeper()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].paymentId").value(paymentId))
                .andExpect(jsonPath("$[0].upiReference").value(UTR))
                .andExpect(jsonPath("$[0].payerName").value("Rishikesh"))
                .andExpect(jsonPath("$[0].amount").value(730))
                .andExpect(jsonPath("$[0].description").value("Agro Store order #" + order));

        mvc.perform(post(SELLER_PAYMENTS + "/{id}/received", paymentId).with(users.shopkeeper()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
        mvc.perform(post(SELLER_PAYMENTS + "/{id}/received", paymentId).with(users.shopkeeper()))
                .andExpect(status().isConflict());
        mvc.perform(get(PAYMENTS + "/{id}", paymentId).with(users.demoFarmer()))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void whenTheMoneyNeverArrivesTheFarmerCanPayAgain() throws Exception {
        long order = checkoutOnline(users.demoFarmer());
        long paymentId = JsonBodies.readId(scanAndPay(users.demoFarmer(), "STORE_ORDER", order, UTR).andReturn(),
                "$.paymentId");

        mvc.perform(post(SELLER_PAYMENTS + "/{id}/not-received", paymentId).with(users.shopkeeper())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"Nothing in my account\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Nothing in my account"));

        createPayment(users.demoFarmer(), "STORE_ORDER", order).andExpect(status().isCreated());
    }

    @Test
    void sellersOnlySeeAndDecideThePaymentsSentToThem() throws Exception {
        long order = checkoutOnline(users.demoFarmer());
        long paymentId = JsonBodies.readId(scanAndPay(users.demoFarmer(), "STORE_ORDER", order, UTR).andReturn(),
                "$.paymentId");

        mvc.perform(get(SELLER_PAYMENTS).with(users.secondShopkeeper())).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(post(SELLER_PAYMENTS + "/{id}/received", paymentId).with(users.secondShopkeeper()))
                .andExpect(status().isNotFound());
        mvc.perform(post(SELLER_PAYMENTS + "/{id}/received", paymentId).with(users.owner()))
                .andExpect(status().isNotFound());
        mvc.perform(get(SELLER_PAYMENTS).with(users.demoFarmer())).andExpect(status().isForbidden());
    }

    @Test
    void aUpiReferencePaysForOnePaymentOnlyAndHasTwelveDigits() throws Exception {
        long order = checkoutOnline(users.demoFarmer());
        scanAndPay(users.demoFarmer(), "STORE_ORDER", order, "12345").andExpect(status().isBadRequest());
        scanAndPay(users.demoFarmer(), "STORE_ORDER", order, "41234567890A").andExpect(status().isBadRequest());
        scanAndPay(users.demoFarmer(), "STORE_ORDER", order, UTR).andExpect(status().isCreated());

        long booking = acceptedBookingId();
        scanAndPay(users.demoFarmer(), "RENTAL_BOOKING", booking, UTR)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "This UPI reference was already used for another payment. Check the 12 digits in your UPI app."));
    }

    @Test
    void theVehicleOwnerConfirmsABookingPaidByScanAndPay() throws Exception {
        long booking = acceptedBookingId();

        createPayment(users.demoFarmer(), "RENTAL_BOOKING", booking)
                .andExpect(jsonPath("$.scanAndPay.upiId").value("vinod.shinde@agriscandemo"));
        long paymentId = JsonBodies.readId(scanAndPay(users.demoFarmer(), "RENTAL_BOOKING", booking, UTR)
                .andExpect(status().isCreated())
                .andReturn(), "$.paymentId");

        RequestPostProcessor vinod = users.signedInAs(TestUsers.BOOKED_OWNER_PHONE);
        mvc.perform(get(SELLER_PAYMENTS).with(vinod))
                .andExpect(jsonPath("$[0].paymentId").value(paymentId))
                .andExpect(jsonPath("$[0].purpose").value("RENTAL_BOOKING"));
        mvc.perform(post(SELLER_PAYMENTS + "/{id}/received", paymentId).with(vinod))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    /** The order follows the payment, and both sides are told, once each step is committed. */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void theOrderWaitsForTheShopkeeperWhoIsNotifiedThenBecomesPaid() throws Exception {
        RequestPostProcessor sunita = users.secondFarmer();
        long farmerId = users.account(TestUsers.SECOND_FARMER_PHONE).farmerId();
        try {
            mvc.perform(post("/api/cart/items").with(sunita).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"productId\": %d, \"quantity\": 1}".formatted(mancozebId()))).andExpect(status().isOk());
            long order = checkoutOnline(sunita);
            long paymentId = JsonBodies.readId(scanAndPay(sunita, "STORE_ORDER", order, "498765432101").andReturn(),
                    "$.paymentId");

            mvc.perform(get("/api/orders/{id}", order).with(sunita))
                    .andExpect(jsonPath("$.status").value("VERIFYING_PAYMENT"));
            mvc.perform(get("/api/shop/orders").param("status", "VERIFYING_PAYMENT").with(users.shopkeeper()))
                    .andExpect(jsonPath("$[0].id").value(order));
            mvc.perform(get("/api/notifications").with(users.shopkeeper()))
                    .andExpect(jsonPath("$.items[0].type").value("PAYMENT_TO_CONFIRM"))
                    .andExpect(jsonPath("$.items[0].route").value("/shop/orders"))
                    .andExpect(jsonPath("$.items[0].body").value(("Sunita Pawar says they paid you ₹280 for Agro Store "
                            + "order #%d (UPI ref 498765432101). Check your bank and confirm it.").formatted(order)));

            mvc.perform(post(SELLER_PAYMENTS + "/{id}/received", paymentId).with(users.shopkeeper()))
                    .andExpect(status().isOk());

            mvc.perform(get("/api/orders/{id}", order).with(sunita)).andExpect(jsonPath("$.status").value("PAID"));
            mvc.perform(get("/api/notifications").with(sunita))
                    .andExpect(jsonPath("$.items[0].type").value("PAYMENT_SUCCEEDED"));
        } finally {
            deleteCommitted(farmerId, users.account(TestUsers.SECOND_FARMER_PHONE).id(),
                    users.account(TestUsers.SHOPKEEPER_PHONE).id());
        }
    }

    private long checkoutOnline(RequestPostProcessor farmer) throws Exception {
        return JsonBodies.readId(mvc.perform(post("/api/cart/checkout").with(farmer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMethod\": \"ONLINE\"}"))
                .andExpect(status().isCreated())
                .andReturn(), "$.orderId");
    }

    private ResultActions createPayment(RequestPostProcessor farmer, String purpose, long referenceId) throws Exception {
        return mvc.perform(post(PAYMENTS).with(farmer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\": \"%s\", \"referenceId\": %d}".formatted(purpose, referenceId)));
    }

    private ResultActions scanAndPay(RequestPostProcessor farmer, String purpose, long referenceId, String upiReference)
            throws Exception {
        return mvc.perform(post(PAYMENTS + "/scan-and-pay").with(farmer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\": \"%s\", \"referenceId\": %d, \"upiReference\": \"%s\"}"
                        .formatted(purpose, referenceId, upiReference)));
    }

    /** The demo farmer's booking of Vinod Shinde's Tata Ace, which Vinod accepted. */
    private long acceptedBookingId() throws Exception {
        return JsonBodies.readId(mvc.perform(get("/api/rentals/bookings").with(users.demoFarmer())).andReturn(),
                "$[1].id");
    }

    private long mancozebId() throws Exception {
        return JsonBodies.readId(mvc.perform(get("/api/store/products/barcode/8904567000058").with(users.demoFarmer()))
                .andReturn(), "$.id");
    }

    private void deleteCommitted(long farmerId, long farmerUserId, long shopkeeperUserId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("delete from payment where farmer_id = ?", farmerId);
            jdbc.update("delete from order_line where order_id in (select id from orders where farmer_id = ?)", farmerId);
            jdbc.update("delete from orders where farmer_id = ?", farmerId);
            jdbc.update("delete from cart_item where cart_id in (select id from cart where farmer_id = ?)", farmerId);
            jdbc.update("delete from cart where farmer_id = ?", farmerId);
            jdbc.update("delete from notification where user_id in (?, ?)", farmerUserId, shopkeeperUserId);
        });
    }
}
