package com.myagree.app.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.myagree.app.common.AgriScanProperties;

/**
 * Issues, rotates and revokes refresh tokens: 256 random bits handed to the browser, of which only the SHA-256 hash
 * is stored. Runs inside the caller's transaction.
 */
@Component
class RefreshTokenStore {

    private static final int TOKEN_BYTES = 32;
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final RefreshTokenRepository repository;
    private final Clock clock;
    private final Duration timeToLive;

    RefreshTokenStore(RefreshTokenRepository repository, Clock clock, AgriScanProperties properties) {
        this.repository = repository;
        this.clock = clock;
        this.timeToLive = properties.security().refreshTokenTtl();
    }

    /** A new token for {@code user}; returns the value for the cookie. */
    String issue(User user) {
        return save(user).value();
    }

    /** The stored token for a cookie value, locked against concurrent refreshes. */
    Optional<RefreshToken> find(String token) {
        return repository.findByTokenHash(hash(token));
    }

    /** Revokes {@code current} in favour of a new token for the same user; returns the new cookie value. */
    String rotate(RefreshToken current) {
        IssuedToken successor = save(current.getUser());
        current.replaceWith(successor.stored(), clock.instant());
        return successor.value();
    }

    void revoke(RefreshToken token) {
        token.revoke(clock.instant());
    }

    /** Ends every session of {@code user}. */
    void revokeAll(User user) {
        Instant now = clock.instant();
        repository.findByUserIdAndRevokedAtIsNull(user.getId()).forEach(token -> token.revoke(now));
    }

    /** Forgets the user's expired tokens, which can no longer be used or reused. */
    void purgeExpired(User user) {
        repository.deleteByUserIdAndExpiresAtBefore(user.getId(), clock.instant());
    }

    private IssuedToken save(User user) {
        byte[] random = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(random);
        String value = TOKEN_ENCODER.encodeToString(random);
        Instant now = clock.instant();
        return new IssuedToken(value, repository.save(new RefreshToken(user, hash(value), now, now.plus(timeToLive))));
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance(HASH_ALGORITHM).digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(HASH_ALGORITHM + " is not available", e);
        }
    }

    /** The cookie value and the stored token it hashes to. */
    private record IssuedToken(String value, RefreshToken stored) {
    }
}
