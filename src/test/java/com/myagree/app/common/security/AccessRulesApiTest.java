package com.myagree.app.common.security;

import static com.myagree.app.support.FixedClockConfiguration.NOW;
import static com.myagree.app.support.TestUsers.DEMO_FARMER_PHONE;
import static com.myagree.app.support.TestUsers.bearer;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.TestUsers;

/** The access rules of docs/architecture/phase-2.md, section 2, and the ApiError bodies of 401 and 403. */
@AgriScanApiTest
class AccessRulesApiTest {

    /** Any answer except the two that mean the security rules turned the request away. */
    private static final Matcher<Integer> ADMITTED = not(anyOf(is(401), is(403)));
    private static final String SIGN_IN_REQUIRED = "Please sign in to continue";
    private static final String SESSION_EXPIRED = "Your session has expired. Please sign in again";
    private static final String ACCESS_DENIED = "Your account does not have access to this feature";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void anonymousCallsAreAskedToSignIn() throws Exception {
        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(SIGN_IN_REQUIRED));
    }

    @Test
    void expiredAccessTokenIsAskedToSignInAgain() throws Exception {
        mvc.perform(get("/api/dashboard").with(users.expiredSessionOf(DEMO_FARMER_PHONE)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SESSION_EXPIRED));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() throws Exception {
        mvc.perform(get("/api/admin/overview").with(forgedAdminToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SESSION_EXPIRED));
    }

    @Test
    void farmersCannotOpenTheAdminOrOwnerPortal() throws Exception {
        for (String portalPath : List.of("/api/admin/overview", "/api/owner/dashboard")) {
            mvc.perform(get(portalPath).with(users.demoFarmer()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"))
                    .andExpect(jsonPath("$.message").value(ACCESS_DENIED));
        }
    }

    @Test
    void ownersAndAdminsCannotOpenTheFarmerApp() throws Exception {
        for (RequestPostProcessor user : List.of(users.owner(), users.admin())) {
            mvc.perform(get("/api/dashboard").with(user))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(ACCESS_DENIED));
        }
        mvc.perform(get("/api/admin/overview").with(users.owner())).andExpect(status().isForbidden());
        mvc.perform(get("/api/owner/dashboard").with(users.admin())).andExpect(status().isForbidden());
    }

    @Test
    void eachPortalAdmitsItsOwnRole() throws Exception {
        mvc.perform(get("/api/dashboard").with(users.demoFarmer())).andExpect(status().isOk());
        mvc.perform(get("/api/owner/dashboard").with(users.owner())).andExpect(status().is(ADMITTED));
        mvc.perform(get("/api/admin/overview").with(users.admin())).andExpect(status().is(ADMITTED));
    }

    @Test
    void notificationsAndTheAccountAreOpenToEveryRole() throws Exception {
        for (RequestPostProcessor user : List.of(users.demoFarmer(), users.owner(), users.admin())) {
            mvc.perform(get("/api/notifications/unread-count").with(user)).andExpect(status().is(ADMITTED));
            mvc.perform(get("/api/auth/me").with(user)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SIGN_IN_REQUIRED));
    }

    @Test
    void signInAndWebhookEndpointsNeedNoAccessToken() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
        mvc.perform(post("/api/payments/webhooks/razorpay")).andExpect(status().is(ADMITTED));
    }

    @Test
    void securityErrorsFollowTheRequestLanguage() throws Exception {
        mvc.perform(get("/api/dashboard").header(HttpHeaders.ACCEPT_LANGUAGE, "mr"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("पुढे जाण्यासाठी कृपया साइन इन करा"));
        mvc.perform(get("/api/dashboard").with(users.owner()).header(HttpHeaders.ACCEPT_LANGUAGE, "hi-IN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("आपके खाते को इस सुविधा की अनुमति नहीं है"));
    }

    @Test
    void farmerRoleWithoutAFarmerProfileIsForbiddenFromFarmerData() throws Exception {
        CurrentUser withoutProfile = new CurrentUser(999, "No Profile", Set.of(Role.FARMER), null, null, Language.EN);

        mvc.perform(get("/api/farmer/me").with(users.signedInAs(withoutProfile)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This account has no farmer profile"));
    }

    /** A well-formed token claiming ADMIN, signed with a secret other than the server's. */
    private static RequestPostProcessor forgedAdminToken() {
        SecretKeySpec otherSecret = new SecretKeySpec(
                "an-attackers-secret-of-at-least-32-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("agriscan")
                .subject("1")
                .issuedAt(NOW)
                .expiresAt(NOW.plus(Duration.ofMinutes(15)))
                .claim("name", "Mallory")
                .claim("roles", List.of(Role.ADMIN.name()))
                .claim("lang", Language.EN.code())
                .build();
        String token = NimbusJwtEncoder.withSecretKey(otherSecret).algorithm(MacAlgorithm.HS256).build()
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return bearer(token);
    }
}
