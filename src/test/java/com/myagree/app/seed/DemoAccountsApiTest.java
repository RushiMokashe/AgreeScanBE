package com.myagree.app.seed;

import static com.myagree.app.support.TestUsers.ADMIN_PASSWORD;
import static com.myagree.app.support.TestUsers.DEMO_FARMER_PHONE;
import static com.myagree.app.support.TestUsers.FARMER_PASSWORD;
import static com.myagree.app.support.TestUsers.OWNER_PASSWORD;
import static com.myagree.app.support.TestUsers.SECOND_FARMER_PHONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.myagree.app.account.Account;
import com.myagree.app.common.security.Role;
import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.TestUsers;

/** The demo accounts of docs/architecture/phase-2.md, section 3, and the farmer profiles linked to them. */
@AgriScanApiTest
class DemoAccountsApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @ParameterizedTest
    @CsvSource({
            "9000000001, AgriScan Admin, ADMIN",
            "9876543210, Rishikesh, FARMER",
            "9876543211, Sunita Pawar, FARMER",
            "9800012345, Rameshwar Patil, VEHICLE_OWNER",
            "9800098765, Shivraj Agro Service, VEHICLE_OWNER",
            "9822233445, Vinod Shinde, VEHICLE_OWNER",
            "9877766554, Balwant Transport Fleet, VEHICLE_OWNER",
            "9000000009, AgriScan Logistics, VEHICLE_OWNER"})
    void everyDemoAccountExistsWithItsRole(String phone, String name, Role role) {
        Account account = users.account(phone);

        assertThat(account.name()).isEqualTo(name);
        assertThat(account.roles()).containsExactly(role);
        assertThat(account.active()).isTrue();
        assertThat(account.farmerId() != null).as("has a farmer profile").isEqualTo(role == Role.FARMER);
        assertThat(account.ownerId() != null).as("has a vehicle-owner profile").isEqualTo(role == Role.VEHICLE_OWNER);
    }

    @ParameterizedTest
    @CsvSource({"9000000001, " + ADMIN_PASSWORD, "9876543211, " + FARMER_PASSWORD, "9822233445, " + OWNER_PASSWORD})
    void demoAccountsSignInWithTheirPublishedPasswords(String phone, String password) throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\": \"%s\", \"password\": \"%s\"}".formatted(phone, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.phone").value(phone));
    }

    @Test
    void rishikeshOwnsThePhaseOneFarm() throws Exception {
        mvc.perform(get("/api/farmer/me").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(users.account(DEMO_FARMER_PHONE).farmerId()))
                .andExpect(jsonPath("$.name").value("Rishikesh"))
                .andExpect(jsonPath("$.location").value("Solapur, Maharashtra"));
    }

    @Test
    void sunitaPawarHasHerOwnFarmerProfile() throws Exception {
        mvc.perform(get("/api/farmer/me").with(users.secondFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(users.account(SECOND_FARMER_PHONE).farmerId()))
                .andExpect(jsonPath("$.name").value("Sunita Pawar"))
                .andExpect(jsonPath("$.location").value("Karmala, Maharashtra"))
                .andExpect(jsonPath("$.season").value("Rabi 2024"))
                .andExpect(jsonPath("$.avatarUrl").value("/images/brand/agriscan-mark.svg"));
    }
}
