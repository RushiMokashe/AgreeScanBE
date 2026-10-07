package com.myagree.app.account;

import java.time.Clock;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.ConflictException;
import com.myagree.app.common.NotFoundException;
import com.myagree.app.common.SearchPatterns;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.security.Role;

/**
 * Creates and maintains user accounts: the admin portal's user management, the farmer and rental features linking
 * their profiles, and demo seeding. Signing in is {@link AuthService}'s job.
 */
@Service
@Transactional(readOnly = true)
public class AccountService {

    /** Largest page {@link #search} returns. */
    public static final int MAX_PAGE_SIZE = 100;
    private static final Sort BY_NAME = Sort.by("name", "id");

    private static final UserMessage NAME_LENGTH = UserMessage.of("common.account.name-length", Account.NAME_MAX_LENGTH);
    private static final UserMessage EMAIL_TOO_LONG = UserMessage.of("common.account.email-too-long", Account.EMAIL_MAX_LENGTH);
    private static final UserMessage ROLES_REQUIRED = UserMessage.of("common.account.roles-required");
    private static final UserMessage PASSWORD_INCORRECT = UserMessage.of("common.account.password-incorrect");
    private static final UserMessage PASSWORD_UNCHANGED = UserMessage.of("common.account.password-unchanged");
    private static final UserMessage CANNOT_DEACTIVATE_SELF = UserMessage.of("common.account.cannot-deactivate-self");
    private static final UserMessage CANNOT_REMOVE_OWN_ADMIN = UserMessage.of("common.account.cannot-remove-own-admin");

    private final UserRepository userRepository;
    private final RefreshTokenStore refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountService(UserRepository userRepository, RefreshTokenStore refreshTokens, PasswordEncoder passwordEncoder,
                          Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public boolean hasAccounts() {
        return userRepository.count() > 0;
    }

    /**
     * @throws NotFoundException when there is no such user
     */
    public Account get(long userId) {
        return AccountMapper.toAccount(findUser(userId));
    }

    /** The account registered with {@code phone}, written in any format {@link NewAccount#phone()} accepts. */
    public Optional<Account> findByPhone(String phone) {
        return PhoneNumbers.normalize(phone).flatMap(userRepository::findByPhone).map(AccountMapper::toAccount);
    }

    /**
     * Accounts ordered by name.
     *
     * @param query matched case-insensitively against name and phone number; {@code null} or blank for all
     * @param role  only accounts holding this role; {@code null} for all
     * @param page  zero-based page number
     * @param size  page size, capped at {@value #MAX_PAGE_SIZE}
     * @throws IllegalArgumentException for a negative page or a size below 1; controllers validate both first
     */
    public Page<Account> search(@Nullable String query, @Nullable Role role, int page, int size) {
        String pattern = query == null || query.isBlank() ? null : SearchPatterns.containsIgnoringCase(query);
        return userRepository.search(pattern, role, PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), BY_NAME))
                .map(AccountMapper::toAccount);
    }

    /** How many accounts hold each role (an account with two roles counts twice); every role is present. */
    public Map<Role, Long> countByRole() {
        Map<Role, Long> counts = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            counts.put(role, 0L);
        }
        userRepository.countUsersByRole().forEach(count -> counts.put(count.role(), count.users()));
        return counts;
    }

    /**
     * Registers an active account.
     *
     * @throws BadRequestException for an invalid phone number, name, role set or password
     * @throws ConflictException   when the phone number is already registered
     */
    @Transactional
    public Account create(NewAccount account) {
        String phone = PhoneNumbers.require(account.phone());
        if (userRepository.existsByPhone(phone)) {
            throw new ConflictException(UserMessage.of("common.account.phone-taken", phone));
        }
        PasswordPolicy.check(account.password());
        User user = new User(phone, requireName(account.name()), normalizeEmail(account.email()),
                passwordEncoder.encode(account.password()), requireRoles(account.roles()), account.preferredLanguage(),
                clock.instant());
        return AccountMapper.toAccount(userRepository.save(user));
    }

    /**
     * Applies {@code update} to an account. Deactivating an account ends all of its sessions. Users cannot
     * deactivate themselves or give up their own ADMIN role, so the admin portal always keeps an administrator.
     *
     * @param actingUserId the signed-in user making the change
     * @throws NotFoundException   when there is no such user
     * @throws BadRequestException for a blank name or an empty role set
     * @throws ConflictException   when users would deactivate themselves or drop their own ADMIN role
     */
    @Transactional
    public Account update(long actingUserId, long userId, AccountUpdate update) {
        User user = findUser(userId);
        if (actingUserId == userId) {
            preventSelfLockout(user, update);
        }
        if (update.name() != null) {
            user.rename(requireName(update.name()));
        }
        if (update.email() != null) {
            user.changeEmail(normalizeEmail(update.email().value()));
        }
        if (update.preferredLanguage() != null) {
            user.changePreferredLanguage(update.preferredLanguage());
        }
        if (update.roles() != null) {
            user.replaceRoles(requireRoles(update.roles()));
        }
        if (update.active() != null && update.active() != user.isActive()) {
            user.changeActive(update.active());
            if (!user.isActive()) {
                refreshTokens.revokeAll(user);
            }
        }
        return AccountMapper.toAccount(user);
    }

    /**
     * Changes the user's own password and ends all of their sessions.
     *
     * @throws BadRequestException when the current password is wrong, or the new one repeats it or breaks the policy
     */
    @Transactional
    public void changePassword(long userId, String currentPassword, String newPassword) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException(PASSWORD_INCORRECT);
        }
        if (newPassword.equals(currentPassword)) {
            throw new BadRequestException(PASSWORD_UNCHANGED);
        }
        setPassword(user, newPassword);
    }

    /**
     * Sets a new password chosen by an administrator and ends all of the user's sessions.
     *
     * @throws NotFoundException   when there is no such user
     * @throws BadRequestException when the password breaks the policy
     */
    @Transactional
    public void resetPassword(long userId, String newPassword) {
        setPassword(findUser(userId), newPassword);
    }

    /**
     * Records the farmer profile the farmer feature created for this user; linking the same profile again is a no-op.
     *
     * @throws NotFoundException     when there is no such user
     * @throws IllegalStateException when the user already has a different farmer profile
     */
    @Transactional
    public Account linkFarmerProfile(long userId, long farmerId) {
        User user = findUser(userId);
        requireUnlinkedOrSame(user.getFarmerId(), farmerId, "farmer", userId);
        user.linkFarmerProfile(farmerId);
        return AccountMapper.toAccount(user);
    }

    /**
     * Records the vehicle-owner profile the rental feature created for this user; linking the same profile again
     * is a no-op.
     *
     * @throws NotFoundException     when there is no such user
     * @throws IllegalStateException when the user already has a different vehicle-owner profile
     */
    @Transactional
    public Account linkOwnerProfile(long userId, long ownerId) {
        User user = findUser(userId);
        requireUnlinkedOrSame(user.getOwnerId(), ownerId, "vehicle-owner", userId);
        user.linkOwnerProfile(ownerId);
        return AccountMapper.toAccount(user);
    }

    /**
     * Records the shop the store feature opened for this user; linking the same shop again is a no-op.
     *
     * @throws NotFoundException     when there is no such user
     * @throws IllegalStateException when the user already keeps a different shop
     */
    @Transactional
    public Account linkShop(long userId, long shopId) {
        User user = findUser(userId);
        requireUnlinkedOrSame(user.getShopId(), shopId, "shop", userId);
        user.linkShop(shopId);
        return AccountMapper.toAccount(user);
    }

    private User findUser(long userId) {
        return userRepository.findById(userId).orElseThrow(() -> NotFoundException.of("User", userId));
    }

    private void setPassword(User user, String password) {
        PasswordPolicy.check(password);
        user.changePasswordHash(passwordEncoder.encode(password));
        refreshTokens.revokeAll(user);
    }

    private static void preventSelfLockout(User self, AccountUpdate update) {
        if (Boolean.FALSE.equals(update.active())) {
            throw new ConflictException(CANNOT_DEACTIVATE_SELF);
        }
        if (update.roles() != null && self.hasRole(Role.ADMIN) && !update.roles().contains(Role.ADMIN)) {
            throw new ConflictException(CANNOT_REMOVE_OWN_ADMIN);
        }
    }

    private static String requireName(String name) {
        String stripped = name.strip();
        if (stripped.isEmpty() || stripped.length() > Account.NAME_MAX_LENGTH) {
            throw new BadRequestException(NAME_LENGTH);
        }
        return stripped;
    }

    private static @Nullable String normalizeEmail(@Nullable String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String stripped = email.strip();
        if (stripped.length() > Account.EMAIL_MAX_LENGTH) {
            throw new BadRequestException(EMAIL_TOO_LONG);
        }
        return stripped;
    }

    private static Set<Role> requireRoles(Collection<Role> roles) {
        if (roles.isEmpty()) {
            throw new BadRequestException(ROLES_REQUIRED);
        }
        return EnumSet.copyOf(roles);
    }

    private static void requireUnlinkedOrSame(@Nullable Long linkedId, long profileId, String profile, long userId) {
        if (linkedId != null && !Objects.equals(linkedId, profileId)) {
            throw new IllegalStateException("User %d is already linked to %s profile %d".formatted(userId, profile, linkedId));
        }
    }
}
