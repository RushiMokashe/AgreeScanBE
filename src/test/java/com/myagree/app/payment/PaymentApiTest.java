package com.myagree.app.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

/**
 * Online payments through the simulated provider, the one the test configuration selects (no provider keys): opening,
 * reusing, confirming, failing and settling payments of store orders and rental bookings.
 */
@AgriScanApiTest
@RecordApplicationEvents
class PaymentApiTest {

    private static final String PAYMENTS = "/api/payments";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Test
    void anOnlineOrderOpensASimulatedCheckoutForItsTotal() throws Exception {
        long order = checkoutOnline(users.demoFarmer());

        long paymentId = JsonBodies.readId(createPayment(users.demoFarmer(), "STORE_ORDER", order)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.purpose").value("STORE_ORDER"))
                .andExpect(jsonPath("$.referenceId").value(order))
                .andExpect(jsonPath("$.amount").value(730))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.description").value("Agro Store order #" + order))
                .andExpect(jsonPath("$.provider").value("SIMULATED"))
                .andExpect(jsonPath("$.status").value("REQUIRES_PAYMENT"))
                .andExpect(jsonPath("$.stripe").value(nullValue()))
                .andExpect(jsonPath("$.razorpay").value(nullValue()))
                .andReturn(), "$.paymentId");

        createPayment(users.demoFarmer(), "STORE_ORDER", order).andExpect(jsonPath("$.paymentId").value(paymentId));
        mvc.perform(get(PAYMENTS + "/{id}", paymentId).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUIRES_PAYMENT"))
                .andExpect(jsonPath("$.failureReason").value(nullValue()));
    }

    @Test
    void theDescriptionIsInTheFarmersLanguage() throws Exception {
        long order = checkoutOnline(users.demoFarmer());

        mvc.perform(post(PAYMENTS).with(users.demoFarmer()).header(HttpHeaders.ACCEPT_LANGUAGE, "mr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purpose\": \"STORE_ORDER\", \"referenceId\": %d}".formatted(order)))
                .andExpect(jsonPath("$.description").value("ॲग्रो स्टोअर ऑर्डर #" + order));
    }

    @Test
    void aPaymentIsDescribedInTheReadersLanguageNotTheOneItOpenedIn() throws Exception {
        long order = checkoutOnline(users.demoFarmer());
        long paymentId = JsonBodies.readId(createPayment(users.demoFarmer(), "STORE_ORDER", order).andReturn(), "$.paymentId");

        mvc.perform(post(PAYMENTS).with(users.demoFarmer()).header(HttpHeaders.ACCEPT_LANGUAGE, "mr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purpose\": \"STORE_ORDER\", \"referenceId\": %d}".formatted(order)))
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.description").value("ॲग्रो स्टोअर ऑर्डर #" + order));
        mvc.perform(get(PAYMENTS + "/{id}", paymentId).with(users.demoFarmer()).header(HttpHeaders.ACCEPT_LANGUAGE, "hi"))
                .andExpect(jsonPath("$.description").value("एग्रो स्टोर ऑर्डर #" + order));
        mvc.perform(confirm(paymentId, "{\"simulatedOutcome\": \"SUCCEEDED\"}").header(HttpHeaders.ACCEPT_LANGUAGE, "mr"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.description").value("ॲग्रो स्टोअर ऑर्डर #" + order));
    }

    @Test
    void aBookingPaymentIsDescribedInTheReadersLanguage() throws Exception {
        long accepted = bookingId(1);
        long paymentId = JsonBodies.readId(createPayment(users.demoFarmer(), "RENTAL_BOOKING", accepted).andReturn(),
                "$.paymentId");

        mvc.perform(get(PAYMENTS + "/{id}", paymentId).with(users.demoFarmer()).header(HttpHeaders.ACCEPT_LANGUAGE, "hi"))
                .andExpect(jsonPath("$.description").value(startsWith("किराया बुकिंग #" + accepted)));
    }

    @Test
    void everyRealChangeOfAPaymentIsPublishedOnceForTheActivityLog() throws Exception {
        long order = checkoutOnline(users.demoFarmer());
        long failed = JsonBodies.readId(createPayment(users.demoFarmer(), "STORE_ORDER", order).andReturn(), "$.paymentId");
        mvc.perform(confirm(failed, "{\"simulatedOutcome\": \"FAILED\"}")).andExpect(status().isOk());
        mvc.perform(confirm(failed, "{\"simulatedOutcome\": \"FAILED\"}")).andExpect(status().isOk());
        long retry = JsonBodies.readId(createPayment(users.demoFarmer(), "STORE_ORDER", order).andReturn(), "$.paymentId");
        paymentService.applyWebhook(PaymentProviderKind.SIMULATED,
                new WebhookOutcome("sim_" + retry, ProviderOutcome.SUCCEEDED));

        assertThat(applicationEvents.stream(PaymentActivity.class))
                .extracting(PaymentActivity::paymentId, PaymentActivity::type, PaymentActivity::source,
                        PaymentActivity::failureReason)
                .containsExactly(
                        tuple(failed, PaymentActivity.Type.OPENED, PaymentActivity.Source.CHECKOUT, null),
                        tuple(failed, PaymentActivity.Type.FAILED, PaymentActivity.Source.CONFIRMATION,
                                "Declined in the test checkout"),
                        tuple(retry, PaymentActivity.Type.OPENED, PaymentActivity.Source.CHECKOUT, null),
                        tuple(retry, PaymentActivity.Type.SUCCEEDED, PaymentActivity.Source.WEBHOOK, null));
        assertThat(applicationEvents.stream(PaymentActivity.class))
                .allSatisfy(activity -> {
                    assertThat(activity.referenceId()).isEqualTo(order);
                    assertThat(activity.amountRupees()).isEqualTo(730);
                    assertThat(activity.providerReference()).isEqualTo("sim_" + activity.paymentId());
                });
    }

    @Test
    void aSimulatedConfirmationNeedsAnOutcome() throws Exception {
        long paymentId = openOrderPayment();

        mvc.perform(confirm(paymentId, "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Choose whether the test payment succeeds or fails"));
    }

    @Test
    void aFailedPaymentLeavesTheOrderPayableWithANewPayment() throws Exception {
        long order = checkoutOnline(users.demoFarmer());
        long failed = JsonBodies.readId(createPayment(users.demoFarmer(), "STORE_ORDER", order).andReturn(), "$.paymentId");

        mvc.perform(confirm(failed, "{\"simulatedOutcome\": \"FAILED\", \"simulatedMethod\": \"UPI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Declined in the test checkout"));
        mvc.perform(confirm(failed, "{\"simulatedOutcome\": \"SUCCEEDED\"}"))
                .andExpect(jsonPath("$.status").value("FAILED"));

        long retry = JsonBodies.readId(createPayment(users.demoFarmer(), "STORE_ORDER", order)
                .andExpect(status().isCreated()).andReturn(), "$.paymentId");
        assertThat(retry).isNotEqualTo(failed);
    }

    @Test
    void anAcceptedBookingCanBePaidButARequestedOneCannot() throws Exception {
        long accepted = bookingId(1);
        long requested = bookingId(0);

        createPayment(users.demoFarmer(), "RENTAL_BOOKING", accepted)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(360))
                .andExpect(jsonPath("$.description").value("Rental booking #" + accepted + " • Tata Ace Gold (छोटा हाथी)"));
        createPayment(users.demoFarmer(), "RENTAL_BOOKING", requested)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This booking can be paid online once the owner accepts it"));
    }

    @Test
    void farmersOnlyPayAndSeeTheirOwn() throws Exception {
        long order = checkoutOnline(users.demoFarmer());
        long paymentId = JsonBodies.readId(createPayment(users.demoFarmer(), "STORE_ORDER", order).andReturn(), "$.paymentId");

        createPayment(users.secondFarmer(), "STORE_ORDER", order)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("There is no such order or booking to pay for"));
        mvc.perform(get(PAYMENTS + "/{id}", paymentId).with(users.secondFarmer())).andExpect(status().isNotFound());
        mvc.perform(post(PAYMENTS + "/{id}/confirm", paymentId).with(users.secondFarmer())).andExpect(status().isNotFound());
        createPayment(users.demoFarmer(), "STORE_ORDER", 999).andExpect(status().isNotFound());
        createPayment(users.owner(), "STORE_ORDER", order).andExpect(status().isForbidden());
    }

    @Test
    void aCashOnDeliveryOrderIsNotPaidOnline() throws Exception {
        long order = JsonBodies.readId(mvc.perform(post("/api/cart/checkout").with(users.demoFarmer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMethod\": \"CASH_ON_DELIVERY\"}"))
                .andReturn(), "$.orderId");

        createPayment(users.demoFarmer(), "STORE_ORDER", order)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This order is not waiting for an online payment"));
    }

    @Test
    void invalidRequestsAreRejected() throws Exception {
        mvc.perform(post(PAYMENTS).with(users.demoFarmer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purpose\": \"GIFT\", \"referenceId\": 1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(PAYMENTS).with(users.demoFarmer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purpose\": \"STORE_ORDER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhooksArePublicButNeedTheProvidersSignature() throws Exception {
        mvc.perform(post(PAYMENTS + "/webhooks/stripe").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The webhook signature is missing or invalid"));
        mvc.perform(post(PAYMENTS + "/webhooks/razorpay").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header("X-Razorpay-Signature", "deadbeef"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aWebhookSettlesAPaymentOnce() throws Exception {
        long paymentId = openOrderPayment();
        WebhookOutcome succeeded = new WebhookOutcome("sim_" + paymentId, ProviderOutcome.SUCCEEDED);

        paymentService.applyWebhook(PaymentProviderKind.SIMULATED, succeeded);
        paymentService.applyWebhook(PaymentProviderKind.SIMULATED, succeeded);
        paymentService.applyWebhook(PaymentProviderKind.SIMULATED, new WebhookOutcome("sim_0", ProviderOutcome.SUCCEEDED));

        mvc.perform(get(PAYMENTS + "/{id}", paymentId).with(users.demoFarmer()))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
        mvc.perform(get("/api/notifications").with(users.demoFarmer()))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].type").value("PAYMENT_SUCCEEDED"))
                .andExpect(jsonPath("$.items[0].route").value("/orders"));
    }

    @Test
    void adminsSeeEveryPayment() throws Exception {
        long paymentId = openOrderPayment();

        mvc.perform(get("/api/admin/payments").param("status", "REQUIRES_PAYMENT").with(users.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].id").value(paymentId))
                .andExpect(jsonPath("$.items[0].payerName").value("Rishikesh"))
                .andExpect(jsonPath("$.items[0].payerPhone").value(TestUsers.DEMO_FARMER_PHONE))
                .andExpect(jsonPath("$.items[0].amount").value(730));
        mvc.perform(get("/api/admin/payments").param("status", "SUCCEEDED").with(users.admin()))
                .andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get("/api/admin/payments").with(users.demoFarmer())).andExpect(status().isForbidden());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void aSucceededPaymentMarksTheOrderPaid() throws Exception {
        RequestPostProcessor sunita = users.secondFarmer();
        long farmerId = users.account(TestUsers.SECOND_FARMER_PHONE).farmerId();
        try {
            mvc.perform(post("/api/cart/items").with(sunita).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"productId\": %d, \"quantity\": 1}".formatted(mancozebId()))).andExpect(status().isOk());
            long order = checkoutOnline(sunita);
            long paymentId = JsonBodies.readId(createPayment(sunita, "STORE_ORDER", order).andReturn(), "$.paymentId");

            mvc.perform(post(PAYMENTS + "/{id}/confirm", paymentId).with(sunita).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"simulatedOutcome\": \"SUCCEEDED\", \"simulatedMethod\": \"CARD\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCEEDED"));

            mvc.perform(get("/api/orders/{id}", order).with(sunita)).andExpect(jsonPath("$.status").value("PAID"));
            createPayment(sunita, "STORE_ORDER", order)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("This order is already paid"));
            mvc.perform(get("/api/notifications").with(sunita))
                    .andExpect(jsonPath("$.items[0].type").value("PAYMENT_SUCCEEDED"))
                    .andExpect(jsonPath("$.items[0].body").value("The payment of ₹280 for Agro Store order #%d went through."
                            .formatted(order)));
        } finally {
            deleteCommitted(farmerId, users.account(TestUsers.SECOND_FARMER_PHONE).id());
        }
    }

    private long openOrderPayment() throws Exception {
        return JsonBodies.readId(createPayment(users.demoFarmer(), "STORE_ORDER", checkoutOnline(users.demoFarmer()))
                .andReturn(), "$.paymentId");
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

    private MockHttpServletRequestBuilder confirm(long paymentId, String json) {
        return post(PAYMENTS + "/{id}/confirm", paymentId).with(users.demoFarmer())
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private long bookingId(int index) throws Exception {
        return JsonBodies.readId(mvc.perform(get("/api/rentals/bookings").with(users.demoFarmer())).andReturn(),
                "$[" + index + "].id");
    }

    private long mancozebId() throws Exception {
        return JsonBodies.readId(mvc.perform(get("/api/store/products/barcode/8904567000058").with(users.demoFarmer()))
                .andReturn(), "$.id");
    }

    /** Removes what the committed test left: Sunita's payment, order, cart and notifications. */
    private void deleteCommitted(long farmerId, long userId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("delete from payment where farmer_id = ?", farmerId);
            jdbc.update("delete from order_line where order_id in (select id from orders where farmer_id = ?)", farmerId);
            jdbc.update("delete from orders where farmer_id = ?", farmerId);
            jdbc.update("delete from cart_item where cart_id in (select id from cart where farmer_id = ?)", farmerId);
            jdbc.update("delete from cart where farmer_id = ?", farmerId);
            jdbc.update("delete from notification where user_id = ?", userId);
        });
    }
}
