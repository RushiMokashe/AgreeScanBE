package com.myagree.app.common.media;

import static com.myagree.app.support.FixedClockConfiguration.NOW;
import static com.myagree.app.support.TestUsers.DEMO_FARMER_PHONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.myagree.app.common.ClockConfig;
import com.myagree.app.common.security.SigningKeys;
import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.TestMediaSource;
import com.myagree.app.support.TestUsers;

/** Signed, expiring media URLs (docs/architecture/phase-2.md, D7), served by {@link TestMediaSource}. */
@AgriScanApiTest
class MediaApiTest {

    private static final String LINK_INVALID = "This media link is invalid or has expired";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MediaUrlSigner signer;

    @Autowired
    private SigningKeys signingKeys;

    @Autowired
    private TestUsers users;

    @Test
    void signedUrlServesTheFileWithoutSignIn() throws Exception {
        String url = signer.sign(TestMediaSource.KIND, TestMediaSource.SAMPLE_ID);

        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(TestMediaSource.SAMPLE_PNG))
                .andExpect(header().string("Content-Security-Policy", "sandbox"));
    }

    @Test
    void urlLivesAtLeastADayAndBrowsersMayCacheItPrivatelyUntilThen() throws Exception {
        String url = signer.sign(TestMediaSource.KIND, TestMediaSource.SAMPLE_ID);
        // 10:00 IST on 29 Sep plus a day, rounded up to the next full hour.
        Instant expiry = Instant.parse("2026-09-30T05:00:00Z");

        assertThat(url).startsWith("/api/media/test/sample?exp=" + expiry.getEpochSecond() + "&sig=");
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL,
                        "max-age=%d, private".formatted(Duration.between(NOW, expiry).toSeconds())));
    }

    @Test
    void expiredUrlIsForbidden() throws Exception {
        MediaUrlSigner twoDaysAgo = new MediaUrlSigner(signingKeys,
                Clock.fixed(NOW.minus(Duration.ofDays(2)), ClockConfig.FARM_ZONE));

        mvc.perform(get(twoDaysAgo.sign(TestMediaSource.KIND, TestMediaSource.SAMPLE_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(LINK_INVALID));
    }

    @ParameterizedTest
    @CsvSource({"kind, other", "id, other", "exp, 1790740800", "sig, AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "sig, not*base64"})
    void tamperedUrlIsForbidden(String part, String replacement) throws Exception {
        UriComponents url = UriComponentsBuilder.fromUriString(signer.sign(TestMediaSource.KIND, TestMediaSource.SAMPLE_ID))
                .build();
        String kind = part.equals("kind") ? replacement : TestMediaSource.KIND;
        String id = part.equals("id") ? replacement : TestMediaSource.SAMPLE_ID;

        mvc.perform(get("/api/media/{kind}/{id}", kind, id)
                        .queryParam("exp", part.equals("exp") ? replacement : url.getQueryParams().getFirst("exp"))
                        .queryParam("sig", part.equals("sig") ? replacement : url.getQueryParams().getFirst("sig")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(LINK_INVALID));
    }

    @Test
    void validSignatureForAFileThatDoesNotExistIsNotFound() throws Exception {
        mvc.perform(get(signer.sign(TestMediaSource.KIND, "missing")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Media test/missing not found"));
        mvc.perform(get(signer.sign("unserved-kind", "1")))
                .andExpect(status().isNotFound());
    }

    @Test
    void urlWithoutSignatureIsABadRequest() throws Exception {
        mvc.perform(get("/api/media/{kind}/{id}", TestMediaSource.KIND, TestMediaSource.SAMPLE_ID).queryParam("exp", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anExpiredAccessTokenSentAlongDoesNotStopTheFile() throws Exception {
        mvc.perform(get(signer.sign(TestMediaSource.KIND, TestMediaSource.SAMPLE_ID))
                        .with(users.expiredSessionOf(DEMO_FARMER_PHONE)))
                .andExpect(status().isOk());
    }

    @Test
    void onlyUrlSafeKindsAndIdsCanBeSigned() {
        assertThatIllegalArgumentException().isThrownBy(() -> signer.sign("Scan", "1"));
        assertThatIllegalArgumentException().isThrownBy(() -> signer.sign("scan", "../secrets"));
        assertThatIllegalArgumentException().isThrownBy(() -> signer.sign("scan", ""));
    }
}
