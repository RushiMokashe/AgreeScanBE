package com.myagree.app.rental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.myagree.app.common.ConflictException;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.spi.Payable;
import com.myagree.app.common.spi.PayableResolver;
import com.myagree.app.common.spi.PaymentPurpose;
import com.myagree.app.common.spi.PaymentSettledEvent;
import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.FixedClockConfiguration;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

/** Paying bookings online: the payable a booking describes, and the booking paid once the payment settles. */
@AgriScanApiTest
class RentalPaymentsTest {

    /** Pawar Transport, the owner of the Pune hub's tempo. */
    private static final String PAWAR_PHONE = "9000000012";
    private static final long PAYMENT_ID = 4242;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private List<PayableResolver> resolvers;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void anAcceptedBookingIsPayableByItsFarmerOnly() throws Exception {
        long accepted = myBookingId(1);
        long requested = myBookingId(0);
        long rishikesh = farmerId(TestUsers.DEMO_FARMER_PHONE);

        Payable payable = rentalResolver().resolvePayable(accepted, rishikesh, Language.HI).orElseThrow();

        assertThat(payable.purpose()).isEqualTo(PaymentPurpose.RENTAL_BOOKING);
        assertThat(payable.referenceId()).isEqualTo(accepted);
        assertThat(payable.amountRupees()).isEqualTo(360);
        assertThat(payable.description()).isEqualTo("किराया बुकिंग #" + accepted + " • टाटा ऐस गोल्ड (छोटा हाथी)");
        assertThat(payable.payerName()).isEqualTo("Rishikesh");
        assertThat(payable.payerPhone()).isEqualTo(TestUsers.DEMO_FARMER_PHONE);
        assertThat(rentalResolver().resolvePayable(accepted, farmerId(TestUsers.SECOND_FARMER_PHONE), Language.EN))
                .isEmpty();
        assertThatThrownBy(() -> rentalResolver().resolvePayable(requested, rishikesh, Language.EN))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void aSettledPaymentPaysTheBookingOnceAndTellsTheOwner() throws Exception {
        RequestPostProcessor pawar = users.signedInAs(PAWAR_PHONE);
        long booking = bookPuneTempo();
        try {
            mvc.perform(post("/api/owner/bookings/{id}/accept", booking).with(pawar)).andExpect(status().isOk());

            settle(PaymentPurpose.STORE_ORDER, booking);
            mvc.perform(get("/api/rentals/bookings/{id}", booking).with(users.demoFarmer()))
                    .andExpect(jsonPath("$.paymentStatus").value("UNPAID"));

            settle(PaymentPurpose.RENTAL_BOOKING, booking);
            settle(PaymentPurpose.RENTAL_BOOKING, booking);

            mvc.perform(get("/api/rentals/bookings/{id}", booking).with(users.demoFarmer()))
                    .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                    .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID))
                    .andExpect(jsonPath("$.canPay").value(false));
            mvc.perform(get("/api/notifications").with(pawar))
                    .andExpect(jsonPath("$.items[?(@.type == 'PAYMENT_SUCCEEDED')]", hasSize(1)))
                    .andExpect(jsonPath("$.items[0].type").value("PAYMENT_SUCCEEDED"))
                    .andExpect(jsonPath("$.items[0].body")
                            .value("The payment of ₹390 for Rental booking #%d • Eicher Pro 2049 Tempo went through."
                                    .formatted(booking)));
            assertThatThrownBy(() -> rentalResolver().resolvePayable(booking,
                    farmerId(TestUsers.DEMO_FARMER_PHONE), Language.EN))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("rental.booking.already-paid");
        } finally {
            deleteCommitted(booking, users.account(PAWAR_PHONE).id(), users.account(TestUsers.DEMO_FARMER_PHONE).id());
        }
    }

    private long bookPuneTempo() throws Exception {
        long puneHub = JsonBodies.readId(mvc.perform(get("/api/rentals/hubs").with(users.demoFarmer())).andReturn(),
                "$[1].id");
        long tempo = JsonBodies.readId(mvc.perform(get("/api/rentals").param("hubId", String.valueOf(puneHub))
                .with(users.demoFarmer())).andReturn(), "$.listings[1].id");
        return JsonBodies.readId(mvc.perform(post("/api/rentals/{id}/bookings", tempo).with(users.demoFarmer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(390))
                .andReturn(), "$.id");
    }

    /** Publishes the payment's settlement the way the payment feature does: inside a transaction that commits. */
    private void settle(PaymentPurpose purpose, long booking) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> events.publishEvent(
                new PaymentSettledEvent(purpose, booking, PAYMENT_ID, farmerId(TestUsers.DEMO_FARMER_PHONE), 390,
                        FixedClockConfiguration.NOW)));
    }

    private void deleteCommitted(long booking, long... notifiedUsers) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("delete from rental_booking_event where booking_id = ?", booking);
            jdbc.update("delete from rental_booking where id = ?", booking);
            for (long userId : notifiedUsers) {
                jdbc.update("delete from notification where user_id = ?", userId);
            }
        });
    }

    private PayableResolver rentalResolver() {
        return resolvers.stream()
                .filter(resolver -> resolver.purpose() == PaymentPurpose.RENTAL_BOOKING)
                .findFirst()
                .orElseThrow();
    }

    private long farmerId(String phone) {
        Long farmerId = users.account(phone).farmerId();
        assertThat(farmerId).isNotNull();
        return farmerId;
    }

    private long myBookingId(int index) throws Exception {
        return JsonBodies.readId(mvc.perform(get("/api/rentals/bookings").with(users.demoFarmer())).andReturn(),
                "$[" + index + "].id");
    }
}
