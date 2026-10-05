package com.myagree.app.common;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Application settings under the {@code agriscan.*} prefix. Secrets come from the environment variables listed in
 * application.properties and docs/architecture/phase-2.md; every one of them may be unset.
 *
 * @param uploadsDir         directory where uploaded crop photos are stored
 * @param corsAllowedOrigins browser origins allowed to call {@code /api/**} directly (the Vite dev server)
 * @param security           sign-in tokens and the refresh cookie
 * @param payments           online payment providers
 * @param assistant          the voice assistant's language model
 */
@ConfigurationProperties("agriscan")
@Validated
public record AgriScanProperties(
        @NotNull Path uploadsDir,
        @NotNull List<String> corsAllowedOrigins,
        @NotNull @Valid Security security,
        @NotNull @Valid Payments payments,
        @NotNull @Valid Assistant assistant) {

    /**
     * @param jwtSecret       signs access tokens and media URLs; at least 32 bytes, or blank for a random secret per start
     * @param cookieSecure    send the refresh cookie over HTTPS only; keep {@code false} for http://localhost
     * @param accessTokenTtl  how long an access token is accepted
     * @param refreshTokenTtl how long a refresh token (and its cookie) can renew the session
     */
    public record Security(
            @NotNull String jwtSecret,
            boolean cookieSecure,
            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl) {
    }

    /**
     * @param provider which provider takes online payments
     * @param stripe   Stripe keys (test keys start with {@code sk_test_} / {@code pk_test_})
     * @param razorpay Razorpay keys (test keys start with {@code rzp_test_})
     */
    public record Payments(@NotNull Provider provider, @NotNull @Valid Stripe stripe, @NotNull @Valid Razorpay razorpay) {

        /** {@code AUTO} picks Stripe when it is configured, else Razorpay when it is, else {@code SIMULATED}. */
        public enum Provider {
            AUTO,
            STRIPE,
            RAZORPAY,
            SIMULATED
        }

        public record Stripe(@NotNull String secretKey, @NotNull String publishableKey, @NotNull String webhookSecret) {

            /** Both API keys are set; the webhook secret is only needed to accept webhooks. */
            public boolean isConfigured() {
                return StringUtils.hasText(secretKey) && StringUtils.hasText(publishableKey);
            }
        }

        public record Razorpay(@NotNull String keyId, @NotNull String keySecret, @NotNull String webhookSecret) {

            /** Both API keys are set; the webhook secret is only needed to accept webhooks. */
            public boolean isConfigured() {
                return StringUtils.hasText(keyId) && StringUtils.hasText(keySecret);
            }
        }
    }

    /** @param anthropic Claude, which answers when an API key is set; the rule-based engine answers otherwise */
    public record Assistant(@NotNull @Valid Anthropic anthropic) {

        /**
         * @param apiKey  Anthropic API key; blank disables Claude
         * @param model   Claude model id
         * @param timeout upper bound for one Claude request before the rule-based engine takes over
         */
        public record Anthropic(@NotNull String apiKey, @NotBlank String model, @NotNull Duration timeout) {

            public boolean isConfigured() {
                return StringUtils.hasText(apiKey);
            }
        }
    }
}
