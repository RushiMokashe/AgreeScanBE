package com.myagree.app.account;

import static com.myagree.app.account.AuthRequests.REFRESH_COOKIE;
import static com.myagree.app.account.AuthRequests.login;
import static com.myagree.app.account.AuthRequests.logout;
import static com.myagree.app.account.AuthRequests.refresh;
import static com.myagree.app.account.AuthRequests.refreshCookieOf;
import static com.myagree.app.support.FixedClockConfiguration.NOW;
import static com.myagree.app.support.TestUsers.ADMIN_PHONE;
import static com.myagree.app.support.TestUsers.DEMO_FARMER_PHONE;
import static com.myagree.app.support.TestUsers.FARMER_PASSWORD;
import static com.myagree.app.support.TestUsers.SECOND_FARMER_PHONE;
import static com.myagree.app.support.TestUsers.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

/** Signing in, renewing and ending sessions (docs/architecture/phase-2.md, D2). */
@AgriScanApiTest
class AuthApiTest {

    private static final Duration ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(15);
    private static final int REFRESH_COOKIE_MAX_AGE = (int) Duration.ofDays(30).toSeconds();
    private static final String BAD_CREDENTIALS = "Incorrect phone number or password";
    private static final String SESSION_ENDED = "Your session has ended. Please sign in again";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private AccountService accountService;

    @Test
    void loginReturnsAnAccessTokenAndSetsTheRefreshCookie() throws Exception {
        MvcResult login = mvc.perform(login(DEMO_FARMER_PHONE, FARMER_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.expiresAt").value(NOW.plus(ACCESS_TOKEN_LIFETIME).toString()))
                .andExpect(jsonPath("$.user.id").value(users.account(DEMO_FARMER_PHONE).id()))
                .andExpect(jsonPath("$.user.name").value("Rishikesh"))
                .andExpect(jsonPath("$.user.phone").value(DEMO_FARMER_PHONE))
                .andExpect(jsonPath("$.user.email").value(nullValue()))
                .andExpect(jsonPath("$.user.roles", contains("FARMER")))
                .andExpect(jsonPath("$.user.preferredLanguage").value("en"))
                .andExpect(jsonPath("$.user.farmerId").isNumber())
                .andExpect(jsonPath("$.user.ownerId").value(nullValue()))
                .andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
                .andExpect(cookie().sameSite(REFRESH_COOKIE, "Strict"))
                .andExpect(cookie().path(REFRESH_COOKIE, "/api/auth"))
                .andExpect(cookie().maxAge(REFRESH_COOKIE, REFRESH_COOKIE_MAX_AGE))
                .andExpect(cookie().secure(REFRESH_COOKIE, false))
                .andReturn();

        mvc.perform(get("/api/dashboard").with(bearer(JsonBodies.read(login, "$.accessToken"))))
                .andExpect(status().isOk());
        assertThat(users.account(DEMO_FARMER_PHONE).lastLoginAt()).isEqualTo(NOW);
    }

    @ParameterizedTest
    @ValueSource(strings = {"+91 98765 43210", "+919876543210", "98765-43210", "919876543210"})
    void loginAcceptsTheMobileNumberInCommonFormats(String phone) throws Exception {
        mvc.perform(login(phone, FARMER_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.phone").value(DEMO_FARMER_PHONE));
    }

    @Test
    void wrongPasswordAndUnknownNumberGetTheSameAnswer() throws Exception {
        mvc.perform(login(DEMO_FARMER_PHONE, "Wrong@123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(BAD_CREDENTIALS))
                .andExpect(cookie().doesNotExist(REFRESH_COOKIE));
        mvc.perform(login("9123456780", FARMER_PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(BAD_CREDENTIALS))
                .andExpect(cookie().doesNotExist(REFRESH_COOKIE));
    }

    @Test
    void loginRejectsSomethingThatIsNotAMobileNumber() throws Exception {
        mvc.perform(login("12345", FARMER_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Enter a valid 10-digit mobile number"));
    }

    @Test
    void loginNeedsBothPhoneAndPassword() throws Exception {
        mvc.perform(login(" ", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("phone must not be blank")))
                .andExpect(jsonPath("$.message", containsString("password must not be blank")));
    }

    @Test
    void deactivatedAccountCannotSignIn() throws Exception {
        deactivate(SECOND_FARMER_PHONE);

        mvc.perform(login(SECOND_FARMER_PHONE, FARMER_PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This account has been deactivated. Please contact AgriScan support"))
                .andExpect(cookie().doesNotExist(REFRESH_COOKIE));
    }

    @Test
    void loginErrorsFollowTheRequestLanguage() throws Exception {
        mvc.perform(login(DEMO_FARMER_PHONE, "Wrong@123").header(HttpHeaders.ACCEPT_LANGUAGE, "mr-IN,mr;q=0.9,en;q=0.8"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("फोन नंबर किंवा पासवर्ड चुकीचा आहे"));
        mvc.perform(login(DEMO_FARMER_PHONE, "Wrong@123").header(HttpHeaders.ACCEPT_LANGUAGE, "hi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("फ़ोन नंबर या पासवर्ड गलत है"));
    }

    @Test
    void refreshIssuesANewAccessTokenAndRotatesTheCookie() throws Exception {
        Cookie first = signIn(DEMO_FARMER_PHONE);

        MvcResult refreshed = mvc.perform(refresh(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.expiresAt").value(NOW.plus(ACCESS_TOKEN_LIFETIME).toString()))
                .andExpect(jsonPath("$.user.phone").value(DEMO_FARMER_PHONE))
                .andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
                .andExpect(cookie().maxAge(REFRESH_COOKIE, REFRESH_COOKIE_MAX_AGE))
                .andReturn();
        Cookie second = refreshCookieOf(refreshed);

        assertThat(second.getValue()).isNotEqualTo(first.getValue());
        mvc.perform(get("/api/dashboard").with(bearer(JsonBodies.read(refreshed, "$.accessToken"))))
                .andExpect(status().isOk());
        mvc.perform(refresh(second)).andExpect(status().isOk());
    }

    @Test
    void reusingARotatedRefreshTokenEndsEverySessionOfTheUser() throws Exception {
        Cookie phone = signIn(DEMO_FARMER_PHONE);
        Cookie laptop = signIn(DEMO_FARMER_PHONE);
        Cookie rotated = refreshCookieOf(mvc.perform(refresh(phone)).andExpect(status().isOk()).andReturn());

        mvc.perform(refresh(phone))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(SESSION_ENDED))
                .andExpect(cookie().value(REFRESH_COOKIE, ""))
                .andExpect(cookie().maxAge(REFRESH_COOKIE, 0));
        mvc.perform(refresh(rotated)).andExpect(status().isUnauthorized());
        mvc.perform(refresh(laptop)).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshNeedsTheRequestedWithHeader() throws Exception {
        Cookie session = signIn(DEMO_FARMER_PHONE);

        mvc.perform(post("/api/auth/refresh").cookie(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Refreshing a session requires the header X-Requested-With: agriscan"));
        mvc.perform(refresh(session)).andExpect(status().isOk());
    }

    @Test
    void refreshWithoutOrWithAnUnknownCookieIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/refresh")
                        .header(AuthController.REQUESTED_WITH_HEADER, AuthController.REQUESTED_WITH_VALUE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SESSION_ENDED));
        mvc.perform(refresh(new Cookie(REFRESH_COOKIE, "made-up-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SESSION_ENDED));
    }

    @Test
    void deactivatedAccountCannotRefresh() throws Exception {
        Cookie session = signIn(SECOND_FARMER_PHONE);
        deactivate(SECOND_FARMER_PHONE);

        mvc.perform(refresh(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SESSION_ENDED));
    }

    /** The frontend sends its last access token on every call, and it has usually expired by the time it refreshes. */
    @Test
    void anExpiredAccessTokenDoesNotStopSignInRefreshOrSignOut() throws Exception {
        mvc.perform(login(DEMO_FARMER_PHONE, FARMER_PASSWORD).with(users.expiredSessionOf(DEMO_FARMER_PHONE)))
                .andExpect(status().isOk());
        Cookie session = signIn(DEMO_FARMER_PHONE);

        MvcResult refreshed = mvc.perform(refresh(session).with(users.expiredSessionOf(DEMO_FARMER_PHONE)))
                .andExpect(status().isOk())
                .andReturn();

        mvc.perform(logout(refreshCookieOf(refreshed)).with(users.expiredSessionOf(DEMO_FARMER_PHONE)))
                .andExpect(status().isNoContent());
    }

    @Test
    void logoutEndsTheSessionAndClearsTheCookie() throws Exception {
        Cookie session = signIn(DEMO_FARMER_PHONE);

        mvc.perform(logout(session))
                .andExpect(status().isNoContent())
                .andExpect(cookie().value(REFRESH_COOKIE, ""))
                .andExpect(cookie().maxAge(REFRESH_COOKIE, 0))
                .andExpect(cookie().path(REFRESH_COOKIE, "/api/auth"));
        mvc.perform(refresh(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void aSignedOutSessionIsRefusedWithoutEndingTheOthers() throws Exception {
        Cookie phone = signIn(DEMO_FARMER_PHONE);
        Cookie laptop = signIn(DEMO_FARMER_PHONE);
        mvc.perform(logout(phone)).andExpect(status().isNoContent());

        mvc.perform(refresh(phone))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SESSION_ENDED));
        mvc.perform(refresh(laptop)).andExpect(status().isOk());
    }

    @Test
    void logoutWithoutASessionStillClearsTheCookie() throws Exception {
        mvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(REFRESH_COOKIE, 0));
    }

    private Cookie signIn(String phone) throws Exception {
        return refreshCookieOf(mvc.perform(login(phone, FARMER_PASSWORD)).andExpect(status().isOk()).andReturn());
    }

    private void deactivate(String phone) {
        long adminId = users.account(ADMIN_PHONE).id();
        accountService.update(adminId, users.account(phone).id(), new AccountUpdate(null, null, null, false, null));
    }
}
