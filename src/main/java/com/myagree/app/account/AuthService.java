package com.myagree.app.account;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.account.dto.AuthSessionResponse;
import com.myagree.app.account.dto.ChangePasswordRequest;
import com.myagree.app.account.dto.CurrentUserResponse;
import com.myagree.app.account.dto.UpdateProfileRequest;
import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.ForbiddenException;
import com.myagree.app.common.UnauthorizedException;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.security.AccessToken;
import com.myagree.app.common.security.AccessTokenIssuer;

/**
 * Sessions (docs/architecture/phase-2.md, D2): signing in with phone and password, renewing the 15-minute access
 * token with the rotating refresh token, signing out, and the signed-in user's own profile.
 */
@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final UserMessage BAD_CREDENTIALS = UserMessage.of("common.auth.bad-credentials");
    private static final UserMessage ACCOUNT_DEACTIVATED = UserMessage.of("common.auth.account-deactivated");

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final RefreshTokenStore refreshTokens;
    private final AccessTokenIssuer accessTokens;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    /** Checked when the phone number is unknown, so that answer takes as long as a wrong password. */
    private final String decoyPasswordHash;

    public AuthService(UserRepository userRepository, AccountService accountService, RefreshTokenStore refreshTokens,
                       AccessTokenIssuer accessTokens, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.decoyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Starts a session and records the sign-in time.
     *
     * @throws BadRequestException   when {@code phone} is not a mobile number
     * @throws UnauthorizedException for an unknown phone number or a wrong password (indistinguishably)
     * @throws ForbiddenException    when the account has been deactivated
     */
    public SignedInSession login(String phone, String password) {
        Optional<User> account = userRepository.findByPhone(PhoneNumbers.require(phone));
        if (account.isEmpty()) {
            passwordEncoder.matches(password, decoyPasswordHash);
            throw new UnauthorizedException(BAD_CREDENTIALS);
        }
        User user = account.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException(BAD_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new ForbiddenException(ACCOUNT_DEACTIVATED);
        }
        if (passwordEncoder.upgradeEncoding(user.getPasswordHash())) {
            user.changePasswordHash(passwordEncoder.encode(password));
        }
        user.recordLogin(clock.instant());
        refreshTokens.purgeExpired(user);
        return signIn(user, refreshTokens.issue(user));
    }

    /**
     * Exchanges a refresh token for a new access token and a new refresh token. A token that was already exchanged
     * shows the cookie was copied: every session of the user ends, and the revocation outlives the failed request.
     * A token ended by sign-out, a password change or deactivation is refused without touching other sessions, so a
     * device that was signed out cannot end the session the user has just started elsewhere.
     *
     * @throws InvalidSessionException when the token is missing, unknown, revoked, expired or reused, or the account
     *                                 is deactivated
     */
    @Transactional(noRollbackFor = InvalidSessionException.class)
    public SignedInSession refresh(@Nullable String refreshToken) {
        RefreshToken current = Optional.ofNullable(refreshToken)
                .flatMap(refreshTokens::find)
                .orElseThrow(InvalidSessionException::new);
        User user = current.getUser();
        if (current.isReplaced()) {
            refreshTokens.revokeAll(user);
            log.warn("Refresh token {} of user {} was used again after it was exchanged; all of the user's sessions "
                    + "are revoked", current.getId(), user.getId());
            throw new InvalidSessionException();
        }
        if (current.isRevoked() || current.isExpired(clock.instant()) || !user.isActive()) {
            throw new InvalidSessionException();
        }
        return signIn(user, refreshTokens.rotate(current));
    }

    /** Ends the session of {@code refreshToken}; unknown or ended sessions are ignored. */
    public void logout(@Nullable String refreshToken) {
        Optional.ofNullable(refreshToken).flatMap(refreshTokens::find).ifPresent(refreshTokens::revoke);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(long userId) {
        return AccountMapper.toCurrentUserResponse(accountService.get(userId));
    }

    /** Applies the user's own changes to name, email and preferred language. */
    public CurrentUserResponse updateProfile(long userId, UpdateProfileRequest request) {
        AccountUpdate update = AccountUpdate.profile(request.name(), request.email(), request.preferredLanguage());
        return AccountMapper.toCurrentUserResponse(accountService.update(userId, userId, update));
    }

    /**
     * Changes the user's password and signs every other device out.
     *
     * @return a new refresh token that keeps this device signed in
     * @throws BadRequestException when the current password is wrong, or the new one repeats it or breaks the policy
     */
    public String changePassword(long userId, ChangePasswordRequest request) {
        accountService.changePassword(userId, request.currentPassword(), request.newPassword());
        return refreshTokens.issue(userRepository.getReferenceById(userId));
    }

    private SignedInSession signIn(User user, String refreshToken) {
        Account account = AccountMapper.toAccount(user);
        AccessToken accessToken = accessTokens.issue(account.toCurrentUser());
        AuthSessionResponse response = new AuthSessionResponse(accessToken.value(), accessToken.expiresAt(),
                AccountMapper.toCurrentUserResponse(account));
        return new SignedInSession(response, refreshToken);
    }
}
