package com.myagree.app.account;

import static com.myagree.app.account.AuthRequests.login;
import static com.myagree.app.support.TestUsers.DEMO_FARMER_PHONE;
import static com.myagree.app.support.TestUsers.FARMER_PASSWORD;
import static com.myagree.app.support.TestUsers.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.myagree.app.common.security.NativeClient;
import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.JsonBodies;

/**
 * Sign-in for the installed Android app, which keeps its refresh token itself and exchanges it in a header
 * ({@link NativeClient}), and the CORS rules that let the app's web view call the API at all.
 */
@AgriScanApiTest
class NativeAppAuthApiTest {

    /** Where Capacitor serves the app from on Android. */
    private static final String APP_ORIGIN = "https://localhost";

    @Autowired
    private MockMvc mvc;

    @Test
    void theAppGetsItsRefreshTokenInAHeaderAndNoCookie() throws Exception {
        mvc.perform(nativeLogin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().exists(NativeClient.REFRESH_TOKEN_HEADER))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void theAppRefreshesWithItsHeaderAndEveryRefreshRotatesTheToken() throws Exception {
        String first = signIn();

        MvcResult refreshed = mvc.perform(nativeRefresh(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andReturn();
        String second = refreshTokenOf(refreshed);

        assertThat(second).isNotEqualTo(first);
        mvc.perform(nativeRefresh(second)).andExpect(status().isOk());
        mvc.perform(nativeRefresh(first)).andExpect(status().isUnauthorized());
    }

    @Test
    void anAppRefreshWithoutItsTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/refresh").header(NativeClient.CLIENT_HEADER, NativeClient.NATIVE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signingOutEndsTheAppsSession() throws Exception {
        String token = signIn();

        mvc.perform(post("/api/auth/logout")
                        .header(NativeClient.CLIENT_HEADER, NativeClient.NATIVE)
                        .header(NativeClient.REFRESH_TOKEN_HEADER, token))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        mvc.perform(nativeRefresh(token)).andExpect(status().isUnauthorized());
    }

    @Test
    void aPasswordChangeHandsTheAppItsNewToken() throws Exception {
        MvcResult signedIn = mvc.perform(nativeLogin()).andExpect(status().isOk()).andReturn();

        MvcResult changed = mvc.perform(post("/api/auth/me/password")
                        .with(bearer(JsonBodies.read(signedIn, "$.accessToken")))
                        .header(NativeClient.CLIENT_HEADER, NativeClient.NATIVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"%s\", \"newPassword\": \"Harvest2026\"}"
                                .formatted(FARMER_PASSWORD)))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andReturn();

        mvc.perform(nativeRefresh(refreshTokenOf(changed))).andExpect(status().isOk());
    }

    @Test
    void theAppsOriginMayCallTheApiAndReadTheTokenHeader() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, APP_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type, x-agriscan-client"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, APP_ORIGIN));
        mvc.perform(nativeLogin().header(HttpHeaders.ORIGIN, APP_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        containsString(NativeClient.REFRESH_TOKEN_HEADER)));
    }

    @Test
    void otherOriginsMayNot() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    private String signIn() throws Exception {
        return refreshTokenOf(mvc.perform(nativeLogin()).andExpect(status().isOk()).andReturn());
    }

    private static MockHttpServletRequestBuilder nativeLogin() {
        return login(DEMO_FARMER_PHONE, FARMER_PASSWORD).header(NativeClient.CLIENT_HEADER, NativeClient.NATIVE);
    }

    private static MockHttpServletRequestBuilder nativeRefresh(String refreshToken) {
        return post("/api/auth/refresh")
                .header(NativeClient.CLIENT_HEADER, NativeClient.NATIVE)
                .header(NativeClient.REFRESH_TOKEN_HEADER, refreshToken);
    }

    private static String refreshTokenOf(MvcResult result) {
        String token = result.getResponse().getHeader(NativeClient.REFRESH_TOKEN_HEADER);
        assertThat(token).as("the %s response header", NativeClient.REFRESH_TOKEN_HEADER).isNotBlank();
        return token;
    }
}
