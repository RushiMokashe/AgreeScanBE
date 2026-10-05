package com.myagree.app.account;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.jspecify.annotations.Nullable;

/**
 * One refresh token of a user's session, stored only as its SHA-256 hash. Every refresh replaces the token with a
 * new one; a replaced token that shows up again means it was copied, so all of the user's tokens are revoked. A token
 * revoked for another reason (sign-out, password change, deactivation) is simply refused.
 */
@Entity
public class RefreshToken {

    /** Hex-encoded SHA-256. */
    static final int HASH_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true, length = HASH_LENGTH)
    private String tokenHash;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private @Nullable Instant revokedAt;

    /** The token issued in exchange for this one, when it was used to refresh. */
    private @Nullable Long replacedById;

    protected RefreshToken() {
    }

    RefreshToken(User user, String tokenHash, Instant issuedAt, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    boolean isRevoked() {
        return revokedAt != null;
    }

    /** Whether this token was already exchanged for a successor. */
    boolean isReplaced() {
        return replacedById != null;
    }

    boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    void replaceWith(RefreshToken successor, Instant now) {
        revoke(now);
        replacedById = successor.getId();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public @Nullable Instant getRevokedAt() {
        return revokedAt;
    }

    public @Nullable Long getReplacedById() {
        return replacedById;
    }
}
