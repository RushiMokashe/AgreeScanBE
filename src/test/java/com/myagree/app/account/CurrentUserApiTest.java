package com.myagree.app.account;

import static com.myagree.app.account.AuthRequests.REFRESH_COOKIE;
import static com.myagree.app.account.AuthRequests.login;
import static com.myagree.app.account.AuthRequests.refresh;
import static com.myagree.app.account.AuthRequests.refreshCookieOf;
import static com.myagree.app.support.TestUsers.DEMO_FARMER_PHONE;
import static com.myagree.app.support.TestUsers.FARMER_PASSWORD;
import static com.myagree.app.support.TestUsers.OWNER_PHONE;
import static com.myagree.app.support.TestUsers.bearer;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

/** The signed-in user's own account: {@code /api/auth/me} and {@code /api/auth/me/password}. */
@AgriScanApiTest
class CurrentUserApiTest {

    private static final String NEW_PASSWORD = "Harvest2026";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void meDescribesTheSignedInFarmer() throws Exception {
        mvc.perform(get("/api/auth/me").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(users.account(DEMO_FARMER_PHONE).id()))
                .andExpect(jsonPath("$.name").value("Rishikesh"))
                .andExpect(jsonPath("$.phone").value(DEMO_FARMER_PHONE))
                .andExpect(jsonPath("$.email").value(nullValue()))
                .andExpect(jsonPath("$.roles", contains("FARMER")))
                .andExpect(jsonPath("$.preferredLanguage").value("en"))
                .andExpect(jsonPath("$.farmerId").isNumber())
                .andExpect(jsonPath("$.ownerId").value(nullValue()));
    }

    @Test
    void meServesEveryRole() throws Exception {
        mvc.perform(get("/api/auth/me").with(users.owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rameshwar Patil"))
                .andExpect(jsonPath("$.roles", contains("VEHICLE_OWNER")))
                .andExpect(jsonPath("$.farmerId").value(nullValue()));
        mvc.perform(get("/api/auth/me").with(users.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", contains("ADMIN")));
    }

    @Test
    void meNeedsASignedInUser() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please sign in to continue"));
    }

    @Test
    void updateChangesOnlyTheFieldsSent() throws Exception {
        updateMe(users.demoFarmer(), "{\"name\": \" Rishikesh Patil \", \"email\": \"rishikesh@example.com\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rishikesh Patil"))
                .andExpect(jsonPath("$.email").value("rishikesh@example.com"))
                .andExpect(jsonPath("$.preferredLanguage").value("en"));

        updateMe(users.demoFarmer(), "{\"preferredLanguage\": \"mr\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rishikesh Patil"))
                .andExpect(jsonPath("$.email").value("rishikesh@example.com"))
                .andExpect(jsonPath("$.preferredLanguage").value("mr"));
        mvc.perform(get("/api/auth/me").with(users.demoFarmer()))
                .andExpect(jsonPath("$.name").value("Rishikesh Patil"))
                .andExpect(jsonPath("$.preferredLanguage").value("mr"));
    }

    @Test
    void sendingANullEmailRemovesIt() throws Exception {
        updateMe(users.owner(), "{\"email\": \"rameshwar@example.com\"}").andExpect(status().isOk());

        updateMe(users.owner(), "{\"email\": null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(nullValue()))
                .andExpect(jsonPath("$.name").value("Rameshwar Patil"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\"name\": \"   \"}                  | name must not be blank",
            "{\"email\": \"not-an-email\"}        | email must be a well-formed email address",
            "{\"preferredLanguage\": \"fr\"}      | Request body is missing or is not valid JSON"})
    void updateRejectsInvalidValues(String body, String message) throws Exception {
        updateMe(users.demoFarmer(), body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void updateRejectsAnOverlongName() throws Exception {
        updateMe(users.demoFarmer(), "{\"name\": \"%s\"}".formatted("x".repeat(Account.NAME_MAX_LENGTH + 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name size must be between 0 and 60"));
    }

    @Test
    void passwordChangeKeepsThisDeviceSignedInAndEndsEveryOtherSession() throws Exception {
        MvcResult thisDevice = signIn(DEMO_FARMER_PHONE, FARMER_PASSWORD);
        Cookie otherDevice = refreshCookieOf(signIn(DEMO_FARMER_PHONE, FARMER_PASSWORD));

        MvcResult changed = changePassword(bearer(JsonBodies.read(thisDevice, "$.accessToken")), FARMER_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isNoContent())
                .andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
                .andReturn();

        mvc.perform(refresh(refreshCookieOf(thisDevice))).andExpect(status().isUnauthorized());
        mvc.perform(refresh(otherDevice)).andExpect(status().isUnauthorized());
        // The devices that were signed out do not take this device's new session down with them.
        mvc.perform(refresh(refreshCookieOf(changed))).andExpect(status().isOk());
        mvc.perform(login(DEMO_FARMER_PHONE, FARMER_PASSWORD)).andExpect(status().isUnauthorized());
        mvc.perform(login(DEMO_FARMER_PHONE, NEW_PASSWORD)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Wrong@123  | Harvest2026  | Current password is incorrect",
            "Farmer@123 | Farmer@123   | Choose a password different from the current one",
            "Farmer@123 | Crop123      | Password must be 8 to 64 characters long",
            "Farmer@123 | HarvestTime  | Password must contain at least one letter and one digit",
            "Farmer@123 | 2026202620   | Password must contain at least one letter and one digit"})
    void passwordChangeFollowsTheRules(String currentPassword, String newPassword, String message) throws Exception {
        changePassword(users.demoFarmer(), currentPassword, newPassword)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(cookie().doesNotExist(REFRESH_COOKIE));
        mvc.perform(login(DEMO_FARMER_PHONE, FARMER_PASSWORD)).andExpect(status().isOk());
    }

    @Test
    void passwordRulesAreExplainedInTheRequestLanguage() throws Exception {
        changePassword(users.signedInAs(OWNER_PHONE), TestUsers.OWNER_PASSWORD, "Crop123", "mr")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("पासवर्ड 8 ते 64 अक्षरांचा असावा"));
        changePassword(users.signedInAs(OWNER_PHONE), TestUsers.OWNER_PASSWORD, "Crop123", "hi")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("पासवर्ड 8 से 64 अक्षरों का होना चाहिए"));
    }

    private ResultActions updateMe(RequestPostProcessor user, String body) throws Exception {
        return mvc.perform(patch("/api/auth/me").with(user).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions changePassword(RequestPostProcessor user, String currentPassword, String newPassword)
            throws Exception {
        return changePassword(user, currentPassword, newPassword, "en");
    }

    private ResultActions changePassword(RequestPostProcessor user, String currentPassword, String newPassword,
                                         String language) throws Exception {
        return mvc.perform(post("/api/auth/me/password")
                .with(user)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\": \"%s\", \"newPassword\": \"%s\"}".formatted(currentPassword, newPassword)));
    }

    private MvcResult signIn(String phone, String password) throws Exception {
        return mvc.perform(login(phone, password)).andExpect(status().isOk()).andReturn();
    }
}
