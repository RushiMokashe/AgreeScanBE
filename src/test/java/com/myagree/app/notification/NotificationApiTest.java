package com.myagree.app.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.common.security.Role;
import com.myagree.app.common.spi.NotificationType;
import com.myagree.app.common.spi.Notifier;
import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

@AgriScanApiTest
class NotificationApiTest {

    private static final String LIST = "/api/notifications";
    private static final String UNREAD_COUNT = "/api/notifications/unread-count";
    private static final String READ_ALL = "/api/notifications/read-all";
    private static final String STREAM = "/api/notifications/stream";
    private static final String OWNER_ROUTE = "/owner/bookings/7";

    /** An account id no seeded user has, for notifications that are committed and must not reach other tests. */
    private static final long STREAM_USER_ID = 987_654L;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private Notifier notifier;

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void listShowsTheNewestFirstInTheReadersLanguage() throws Exception {
        long ownerId = ownerUserId();
        notifier.notify(ownerId, NotificationType.BOOKING_CANCELLED,
                Map.of(Notifier.FARMER_NAME, "Rishikesh", Notifier.LISTING_NAME, "Tata Ace Gold",
                        Notifier.SLOT, "Tomorrow 6 AM", Notifier.REASON, ""), OWNER_ROUTE);
        notifyBookingRequested(ownerId);

        mvc.perform(get(LIST).with(users.owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items[0].type").value("BOOKING_REQUESTED"))
                .andExpect(jsonPath("$.items[0].title").value("New booking request"))
                .andExpect(jsonPath("$.items[0].body")
                        .value("Rishikesh wants Mahindra 575 DI (45 HP) + Rotavator for Slot: 02:00 PM Today (₹1,300)"))
                .andExpect(jsonPath("$.items[0].route").value(OWNER_ROUTE))
                .andExpect(jsonPath("$.items[0].read").value(false))
                .andExpect(jsonPath("$.items[1].body")
                        .value("Rishikesh cancelled the booking of Tata Ace Gold for Tomorrow 6 AM."));

        mvc.perform(get(LIST).with(users.owner()).header(HttpHeaders.ACCEPT_LANGUAGE, "mr"))
                .andExpect(jsonPath("$.items[0].title").value("नवीन बुकिंग विनंती"))
                .andExpect(jsonPath("$.items[0].body").value(containsString("₹1,300")));
        mvc.perform(get(LIST).with(users.owner()).header(HttpHeaders.ACCEPT_LANGUAGE, "hi"))
                .andExpect(jsonPath("$.items[0].title").value("नया बुकिंग अनुरोध"))
                .andExpect(jsonPath("$.items[1].title").value("बुकिंग रद्द हुई"));
    }

    @Test
    void everyTypeHasTextsInEveryLanguage() throws Exception {
        long ownerId = ownerUserId();
        for (NotificationType type : NotificationType.values()) {
            Map<String, String> params = new HashMap<>();
            type.parameters().forEach(parameter -> params.put(parameter, "x"));
            notifier.notify(ownerId, type, params, OWNER_ROUTE);
        }
        for (Language language : Language.values()) {
            mvc.perform(get(LIST).with(users.owner()).header(HttpHeaders.ACCEPT_LANGUAGE, language.code()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalItems").value(NotificationType.values().length))
                    .andExpect(jsonPath("$.items[?(@.title == '')]").isEmpty())
                    .andExpect(jsonPath("$.items[?(@.body =~ /.*\\{\\d\\}.*/)]").isEmpty());
        }
    }

    @Test
    void pagesAreLimitedAndValidated() throws Exception {
        long ownerId = ownerUserId();
        notifyBookingRequested(ownerId);
        notifyBookingRequested(ownerId);
        notifyBookingRequested(ownerId);

        mvc.perform(get(LIST).param("page", "1").param("size", "2").with(users.owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get(LIST).param("size", "0").with(users.owner())).andExpect(status().isBadRequest());
        mvc.perform(get(LIST).param("size", "51").with(users.owner())).andExpect(status().isBadRequest());
        mvc.perform(get(LIST).param("page", "-1").with(users.owner())).andExpect(status().isBadRequest());
    }

    @Test
    void readingUpdatesTheBadgeAndKeepsTheFirstReadTime() throws Exception {
        long ownerId = ownerUserId();
        notifyBookingRequested(ownerId);
        notifyBookingRequested(ownerId);
        long first = JsonBodies.readId(mvc.perform(get(LIST).with(users.owner())).andReturn(), "$.items[0].id");

        mvc.perform(get(UNREAD_COUNT).with(users.owner())).andExpect(jsonPath("$.count").value(2));
        mvc.perform(post(LIST + "/" + first + "/read").with(users.owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(first))
                .andExpect(jsonPath("$.read").value(true));
        mvc.perform(post(LIST + "/" + first + "/read").with(users.owner())).andExpect(status().isOk());
        mvc.perform(get(UNREAD_COUNT).with(users.owner())).andExpect(jsonPath("$.count").value(1));

        mvc.perform(post(READ_ALL).with(users.owner())).andExpect(status().isNoContent());
        mvc.perform(get(UNREAD_COUNT).with(users.owner())).andExpect(jsonPath("$.count").value(0));
        mvc.perform(get(LIST).with(users.owner())).andExpect(jsonPath("$.items[?(@.read == false)]").isEmpty());
    }

    @Test
    void usersOnlySeeTheirOwnNotifications() throws Exception {
        notifyBookingRequested(ownerUserId());
        long ownersNotification = JsonBodies.readId(mvc.perform(get(LIST).with(users.owner())).andReturn(),
                "$.items[0].id");

        mvc.perform(get(LIST).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get(UNREAD_COUNT).with(users.demoFarmer())).andExpect(jsonPath("$.count").value(0));
        mvc.perform(post(LIST + "/" + ownersNotification + "/read").with(users.demoFarmer()))
                .andExpect(status().isNotFound());
        mvc.perform(post(LIST + "/" + ownersNotification + "/read").with(users.demoFarmer())
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "mr"))
                .andExpect(jsonPath("$.message").value("सूचना " + ownersNotification + " सापडली नाही"));
        mvc.perform(post(READ_ALL).with(users.demoFarmer())).andExpect(status().isNoContent());
        mvc.perform(get(UNREAD_COUNT).with(users.owner())).andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void everyRoleCanReadItsNotificationsButNobodyAnonymously() throws Exception {
        mvc.perform(get(LIST).with(users.admin())).andExpect(status().isOk());
        mvc.perform(get(LIST).with(users.demoFarmer())).andExpect(status().isOk());
        mvc.perform(get(LIST).with(users.owner())).andExpect(status().isOk());
        mvc.perform(get(LIST)).andExpect(status().isUnauthorized());
        mvc.perform(get(STREAM)).andExpect(status().isUnauthorized());
    }

    @Test
    void notifierRejectsMissingParametersAndRoutesOutsideTheApp() {
        long ownerId = ownerUserId();
        assertThatThrownBy(() -> notifier.notify(ownerId, NotificationType.BOOKING_STARTED,
                Map.of(Notifier.OWNER_NAME, "Rameshwar Patil"), "/bookings/7"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Notifier.LISTING_NAME);
        assertThatThrownBy(() -> notifier.notify(ownerId, NotificationType.BOOKING_STARTED, startedParams(),
                "//evil.example/phish"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> notifier.notify(ownerId, NotificationType.BOOKING_STARTED, startedParams(),
                "https://evil.example"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theStreamDeliversCommittedNotificationsInItsLanguageAndNeverRolledBackOnes() throws Exception {
        CurrentUser streamUser = new CurrentUser(STREAM_USER_ID, "Stream Tester", Set.of(Role.FARMER), null, null,
                null, Language.EN);
        MvcResult stream = mvc.perform(get(STREAM).with(users.signedInAs(streamUser))
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "hi"))
                .andExpect(request().asyncStarted())
                .andReturn();
        try {
            TransactionTemplate separate = new TransactionTemplate(transactionManager);
            separate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            separate.executeWithoutResult(rolledBack -> {
                notifier.notify(STREAM_USER_ID, NotificationType.BOOKING_STARTED,
                        Map.of(Notifier.OWNER_NAME, "Never Sent", Notifier.LISTING_NAME, "Ghost Tractor"), "/bookings/1");
                rolledBack.setRollbackOnly();
            });
            separate.executeWithoutResult(committed -> notifier.notify(STREAM_USER_ID, NotificationType.BOOKING_STARTED,
                    startedParams(), "/bookings/7"));

            String events = stream.getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(events)
                    .contains("event:ping")
                    .contains("event:notification")
                    .contains("काम शुरू हुआ")
                    .contains("Rameshwar Patil")
                    .doesNotContain("Ghost Tractor");
        } finally {
            TransactionTemplate cleanup = new TransactionTemplate(transactionManager);
            cleanup.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            cleanup.executeWithoutResult(status -> repository.deleteAll(
                    repository.findByUserIdOrderByCreatedAtDescIdDesc(STREAM_USER_ID, Pageable.unpaged()).getContent()));
        }
    }

    private void notifyBookingRequested(long userId) {
        notifier.notify(userId, NotificationType.BOOKING_REQUESTED,
                Map.of(Notifier.FARMER_NAME, "Rishikesh", Notifier.LISTING_NAME, "Mahindra 575 DI (45 HP) + Rotavator",
                        Notifier.SLOT, "Slot: 02:00 PM Today", Notifier.AMOUNT, "1300"), OWNER_ROUTE);
    }

    private static Map<String, String> startedParams() {
        return Map.of(Notifier.OWNER_NAME, "Rameshwar Patil", Notifier.LISTING_NAME, "Mahindra 575 DI (45 HP)");
    }

    private long ownerUserId() {
        return users.account(TestUsers.OWNER_PHONE).id();
    }
}
