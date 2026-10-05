package com.myagree.app.common.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.myagree.app.common.AgriScanProperties;

/**
 * The server secret ({@code agriscan.security.jwt-secret}, env {@code AGRISCAN_JWT_SECRET}) and the keys derived
 * from it. Without a configured secret a random one is generated per start: fine for development, but a restart
 * then ends every session and invalidates every media URL.
 */
@Component
public class SigningKeys {

    private static final Logger log = LoggerFactory.getLogger(SigningKeys.class);

    /** HS256 needs a key of at least 256 bits. */
    static final int MIN_SECRET_BYTES = 32;
    static final String HMAC_SHA256 = "HmacSHA256";

    private final SecretKey accessTokenKey;

    SigningKeys(AgriScanProperties properties) {
        this.accessTokenKey = new SecretKeySpec(secretBytes(properties.security().jwtSecret()), HMAC_SHA256);
    }

    /** Signs and verifies access tokens (HS256). */
    SecretKey accessTokenKey() {
        return accessTokenKey;
    }

    /**
     * A key for another use of the server secret, independent of the access-token key:
     * HMAC-SHA256(secret, purpose). For example {@code derive("media-url")}.
     */
    public SecretKey derive(String purpose) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(accessTokenKey);
            return new SecretKeySpec(mac.doFinal(purpose.getBytes(StandardCharsets.UTF_8)), HMAC_SHA256);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }

    private static byte[] secretBytes(String configuredSecret) {
        if (!StringUtils.hasText(configuredSecret)) {
            log.warn("AGRISCAN_JWT_SECRET is not set; using a random secret, so sign-ins and media links end when "
                    + "the server restarts");
            byte[] secret = new byte[MIN_SECRET_BYTES];
            new SecureRandom().nextBytes(secret);
            return secret;
        }
        byte[] secret = configuredSecret.getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("AGRISCAN_JWT_SECRET must be at least %d bytes long".formatted(MIN_SECRET_BYTES));
        }
        return secret;
    }
}
