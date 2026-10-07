package com.myagree.app.common.security;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.DispatcherType;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.myagree.app.common.i18n.Messages;

import tools.jackson.databind.json.JsonMapper;

/**
 * Stateless bearer-token security for {@code /api/**}. Access rules (docs/architecture/phase-2.md, section 2):
 * sign-in endpoints, signed media URLs and payment webhooks are public; {@code /api/admin/**} needs ADMIN,
 * {@code /api/owner/**} needs VEHICLE_OWNER, {@code /api/auth/me/**}, {@code /api/notifications/**} and
 * {@code /api/rentals/hubs} any signed-in user, and the rest of {@code /api/**} needs FARMER. Everything else is denied.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    /** These authenticate by other means: password, refresh cookie, URL signature or provider signature. */
    private static final RequestMatcher PUBLIC_ENDPOINTS = anyOf(
            "/api/auth/login", "/api/auth/refresh", "/api/auth/logout", "/api/media/**", "/api/payments/webhooks/**");
    /**
     * Open to every signed-in user, whatever their roles. Owners and admins pick a rental hub from the same list
     * farmers choose from.
     */
    private static final RequestMatcher ANY_ROLE_ENDPOINTS =
            anyOf("/api/auth/me/**", "/api/notifications/**", "/api/rentals/hubs");
    private static final String ROLE_AUTHORITY_PREFIX = "ROLE_";

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JsonMapper jsonMapper, Messages messages) throws Exception {
        SecurityErrorResponses errorResponses = new SecurityErrorResponses(jsonMapper, messages);
        return http
                .cors(Customizer.withDefaults())
                // Browsers never attach bearer tokens by themselves. The refresh cookie is SameSite=Strict, and the
                // refresh endpoint also demands an X-Requested-With header.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers("/api/admin/**").hasRole(Role.ADMIN.name())
                        .requestMatchers("/api/owner/**").hasRole(Role.VEHICLE_OWNER.name())
                        .requestMatchers(ANY_ROLE_ENDPOINTS).authenticated()
                        .requestMatchers("/api/**").hasRole(Role.FARMER.name())
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(bearerTokenOutside(PUBLIC_ENDPOINTS))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesAsAuthorities()))
                        .authenticationEntryPoint(errorResponses)
                        .accessDeniedHandler(errorResponses))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorResponses)
                        .accessDeniedHandler(errorResponses))
                .build();
    }

    /** The development H2 console has its own login, and uses frames and forms without CSRF tokens. */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnBooleanProperty("spring.h2.console.enabled")
    SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(PathRequest.toH2Console())
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .build();
    }

    @Bean
    JwtEncoder jwtEncoder(SigningKeys signingKeys) {
        return NimbusJwtEncoder.withSecretKey(signingKeys.accessTokenKey()).algorithm(MacAlgorithm.HS256).build();
    }

    /** Accepts only HS256 tokens this server issued that carry an expiry still ahead of the application clock. */
    @Bean
    JwtDecoder jwtDecoder(SigningKeys signingKeys, Clock clock) {
        JwtTimestampValidator expiry = new JwtTimestampValidator();
        expiry.setClock(clock);
        expiry.setAllowEmptyExpiryClaim(false);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(signingKeys.accessTokenKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
                List.of(expiry, new JwtIssuerValidator(AccessTokenClaims.ISSUER))));
        return decoder;
    }

    /** BCrypt, stored as "{bcrypt}..." so a stronger algorithm can later re-hash passwords as users sign in. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Reads the bearer token everywhere except on {@code publicEndpoints}: the frontend sends its last access token
     * with every call, and one that has expired must not stop a sign-in, a refresh or a sign-out.
     */
    private static BearerTokenResolver bearerTokenOutside(RequestMatcher publicEndpoints) {
        BearerTokenResolver authorizationHeader = new DefaultBearerTokenResolver();
        return request -> publicEndpoints.matches(request) ? null : authorizationHeader.resolve(request);
    }

    private static RequestMatcher anyOf(String... pathPatterns) {
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();
        return new OrRequestMatcher(Arrays.stream(pathPatterns).<RequestMatcher>map(paths::matcher).toList());
    }

    /** The token's {@code roles} claim becomes {@code ROLE_*} authorities, e.g. FARMER becomes ROLE_FARMER. */
    private static JwtAuthenticationConverter rolesAsAuthorities() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(AccessTokenClaims.ROLES);
        authorities.setAuthorityPrefix(ROLE_AUTHORITY_PREFIX);
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
