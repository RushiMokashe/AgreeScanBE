package com.myagree.app.support;

import java.time.Clock;
import java.time.Duration;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.myagree.app.account.Account;
import com.myagree.app.account.AccountService;
import com.myagree.app.common.AgriScanProperties;
import com.myagree.app.common.security.AccessTokenIssuer;
import com.myagree.app.common.security.CurrentUser;

/**
 * Signs MockMvc requests in with real access tokens, e.g.
 * {@code mvc.perform(get("/api/dashboard").with(users.demoFarmer()))}. A request without {@code with(...)} is
 * anonymous. The demo accounts are those of docs/architecture/phase-2.md, section 3.
 */
@TestComponent
public class TestUsers {

    public static final String ADMIN_PHONE = "9000000001";
    public static final String DEMO_FARMER_PHONE = "9876543210";
    public static final String SECOND_FARMER_PHONE = "9876543211";
    public static final String OWNER_PHONE = "9800012345";
    public static final String ADMIN_PASSWORD = "Admin@123";
    public static final String FARMER_PASSWORD = "Farmer@123";
    public static final String OWNER_PASSWORD = "Owner@123";

    /** Well past the access-token lifetime and the decoder's clock-skew allowance. */
    private static final Duration EXPIRED_TOKEN_AGE = Duration.ofHours(1);

    private final AccountService accountService;
    private final AccessTokenIssuer accessTokenIssuer;
    private final AccessTokenIssuer expiredTokenIssuer;

    TestUsers(AccountService accountService, AccessTokenIssuer accessTokenIssuer, JwtEncoder jwtEncoder, Clock clock,
              AgriScanProperties properties) {
        this.accountService = accountService;
        this.accessTokenIssuer = accessTokenIssuer;
        this.expiredTokenIssuer = new AccessTokenIssuer(jwtEncoder, Clock.offset(clock, EXPIRED_TOKEN_AGE.negated()),
                properties);
    }

    /** Rishikesh, the farmer who owns all phase-1 demo data. */
    public RequestPostProcessor demoFarmer() {
        return signedInAs(DEMO_FARMER_PHONE);
    }

    /** Sunita Pawar, a farmer with no data yet: proves that farmers only see their own records. */
    public RequestPostProcessor secondFarmer() {
        return signedInAs(SECOND_FARMER_PHONE);
    }

    /** Rameshwar Patil, a vehicle owner. */
    public RequestPostProcessor owner() {
        return signedInAs(OWNER_PHONE);
    }

    /** AgriScan Admin. */
    public RequestPostProcessor admin() {
        return signedInAs(ADMIN_PHONE);
    }

    /** Any seeded account. */
    public RequestPostProcessor signedInAs(String phone) {
        return signedInAs(account(phone).toCurrentUser());
    }

    /** Any identity, even one without an account, e.g. a farmer whose profile is missing. */
    public RequestPostProcessor signedInAs(CurrentUser user) {
        return bearer(accessTokenIssuer.issue(user).value());
    }

    /** {@code phone}'s account with an access token that expired an hour ago, as a client sends it before refreshing. */
    public RequestPostProcessor expiredSessionOf(String phone) {
        return bearer(expiredTokenIssuer.issue(account(phone).toCurrentUser()).value());
    }

    public Account account(String phone) {
        return accountService.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("No seeded account has phone " + phone));
    }

    /** Sends {@code accessToken} as the request's bearer token. */
    public static RequestPostProcessor bearer(String accessToken) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            return request;
        };
    }
}
