package com.myagree.app.common.security;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;

import com.myagree.app.common.i18n.Language;

/** How a {@link CurrentUser} is written into, and read back from, an access token's claims. */
final class AccessTokenClaims {

    /** Tokens name this issuer, and only tokens that do are accepted. */
    static final String ISSUER = "agriscan";
    /** Role names, e.g. ["FARMER"]; mapped to {@code ROLE_*} authorities. */
    static final String ROLES = "roles";
    private static final String NAME = "name";
    private static final String FARMER_ID = "fid";
    private static final String OWNER_ID = "oid";
    private static final String LANGUAGE = "lang";

    private AccessTokenClaims() {
    }

    static JwtClaimsSet encode(CurrentUser user, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.userId()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(NAME, user.name())
                .claim(ROLES, user.roles().stream().sorted().map(Role::name).toList())
                .claim(LANGUAGE, user.preferredLanguage().code());
        if (user.farmerId() != null) {
            claims.claim(FARMER_ID, user.farmerId());
        }
        if (user.ownerId() != null) {
            claims.claim(OWNER_ID, user.ownerId());
        }
        return claims.build();
    }

    static CurrentUser decode(Jwt token) {
        Set<Role> roles = token.getClaimAsStringList(ROLES).stream().map(Role::valueOf).collect(Collectors.toSet());
        return new CurrentUser(
                Long.parseLong(token.getSubject()),
                token.getClaimAsString(NAME),
                roles,
                idClaim(token, FARMER_ID),
                idClaim(token, OWNER_ID),
                Language.fromCode(token.getClaimAsString(LANGUAGE)));
    }

    private static @Nullable Long idClaim(Jwt token, String claim) {
        Number id = token.getClaim(claim);
        return id != null ? id.longValue() : null;
    }
}
