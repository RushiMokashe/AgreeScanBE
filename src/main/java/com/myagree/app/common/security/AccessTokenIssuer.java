package com.myagree.app.common.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.myagree.app.common.AgriScanProperties;

/**
 * Issues HS256 access tokens carrying a {@link CurrentUser}: {@code sub} (user id), {@code name}, {@code roles},
 * {@code fid} (farmer id), {@code oid} (owner id), {@code sid} (shop id) and {@code lang}, valid for {@code agriscan.security.access-token-ttl}.
 */
@Component
public class AccessTokenIssuer {

    private static final JwsHeader HS256 = JwsHeader.with(MacAlgorithm.HS256).build();

    private final JwtEncoder encoder;
    private final Clock clock;
    private final Duration timeToLive;

    public AccessTokenIssuer(JwtEncoder encoder, Clock clock, AgriScanProperties properties) {
        this.encoder = encoder;
        this.clock = clock;
        this.timeToLive = properties.security().accessTokenTtl();
    }

    public AccessToken issue(CurrentUser user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(timeToLive);
        String token = encoder.encode(JwtEncoderParameters.from(HS256, AccessTokenClaims.encode(user, issuedAt, expiresAt)))
                .getTokenValue();
        return new AccessToken(token, expiresAt);
    }
}
