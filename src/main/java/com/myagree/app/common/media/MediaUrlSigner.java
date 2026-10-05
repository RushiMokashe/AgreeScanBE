package com.myagree.app.common.media;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.myagree.app.common.security.SigningKeys;

/**
 * Signs and verifies media URLs: HMAC-SHA256 over {@code kind:id:exp} with a key derived from the server secret.
 * {@code exp} is the expiry in epoch seconds; the signature is base64url without padding.
 */
@Component
public class MediaUrlSigner {

    /** A signed URL stays valid at least this long. */
    static final Duration TIME_TO_LIVE = Duration.ofHours(24);
    /**
     * Expiries are rounded up to the next full hour, so a file keeps one URL (and one browser-cache entry) for an
     * hour at a time; each URL is therefore valid for 24 to 25 hours.
     */
    private static final ChronoUnit EXPIRY_ROUNDING = ChronoUnit.HOURS;

    private static final String KEY_PURPOSE = "media-url";
    private static final Pattern KIND = Pattern.compile("[a-z][a-z0-9-]{0,31}");
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final String URL_TEMPLATE = "/api/media/%s/%s?exp=%d&sig=%s";
    private static final Base64.Encoder SIGNATURE_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final SecretKey key;
    private final Clock clock;

    MediaUrlSigner(SigningKeys signingKeys, Clock clock) {
        this.key = signingKeys.derive(KEY_PURPOSE);
        this.clock = clock;
    }

    /**
     * A URL that serves the file without sign-in for at least 24 hours, e.g.
     * {@code /api/media/scan/42?exp=1790640000&sig=...}.
     *
     * @param kind the {@link MediaSource#kind()} that serves the file
     * @param id   the file's id within that kind: 1-64 letters, digits, '-' or '_'. Sign the id of the stored file,
     *             not of its owner, so a replaced photo gets a new URL instead of a stale cached one.
     * @throws IllegalArgumentException for a kind or id outside those alphabets
     */
    public String sign(String kind, String id) {
        requireMatch(KIND, kind, "kind");
        requireMatch(ID, id, "id");
        long expiresAt = clock.instant().plus(TIME_TO_LIVE).truncatedTo(EXPIRY_ROUNDING).plus(1, EXPIRY_ROUNDING)
                .getEpochSecond();
        return URL_TEMPLATE.formatted(kind, id, expiresAt, SIGNATURE_ENCODER.encodeToString(signature(kind, id, expiresAt)));
    }

    /** Whether {@code signature} is this server's signature of exactly these values and the URL has not expired. */
    boolean isValid(String kind, String id, long expiresAt, String signature) {
        if (!clock.instant().isBefore(Instant.ofEpochSecond(expiresAt))) {
            return false;
        }
        byte[] presented;
        try {
            presented = Base64.getUrlDecoder().decode(signature);
        } catch (IllegalArgumentException notBase64Url) {
            return false;
        }
        return MessageDigest.isEqual(signature(kind, id, expiresAt), presented);
    }

    private byte[] signature(String kind, String id, long expiresAt) {
        try {
            Mac mac = Mac.getInstance(key.getAlgorithm());
            mac.init(key);
            return mac.doFinal((kind + ':' + id + ':' + expiresAt).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot sign media URLs with " + key.getAlgorithm(), e);
        }
    }

    private static void requireMatch(Pattern pattern, String value, String name) {
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException("Media %s '%s' must match %s".formatted(name, value, pattern));
        }
    }
}
