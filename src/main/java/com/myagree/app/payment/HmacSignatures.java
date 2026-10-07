package com.myagree.app.payment;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** HMAC-SHA256 signatures as hex, compared in constant time so timing never reveals how much of one matched. */
final class HmacSignatures {

    private static final String ALGORITHM = "HmacSHA256";

    private HmacSignatures() {
    }

    /** Whether {@code signature} is the hex HMAC-SHA256 of {@code payload} under {@code secret}. */
    static boolean matches(String payload, String secret, String signature) {
        byte[] expected = HexFormat.of().formatHex(hmac(payload, secret)).getBytes(StandardCharsets.US_ASCII);
        byte[] presented = signature.strip().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, presented);
    }

    static String sign(String payload, String secret) {
        return HexFormat.of().formatHex(hmac(payload, secret));
    }

    private static byte[] hmac(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot compute " + ALGORITHM, e);
        }
    }
}
