package com.myagree.app.account;

import static com.myagree.app.support.FixedClockConfiguration.NOW;
import static com.myagree.app.support.TestUsers.ADMIN_PHONE;
import static com.myagree.app.support.TestUsers.DEMO_FARMER_PHONE;
import static com.myagree.app.support.TestUsers.OWNER_PHONE;
import static com.myagree.app.support.TestUsers.SECOND_FARMER_PHONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import com.myagree.app.common.ApiException;
import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.Change;
import com.myagree.app.common.ConflictException;
import com.myagree.app.common.NotFoundException;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.Messages;
import com.myagree.app.common.security.Role;
import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.TestUsers;

/** User management as the admin feature uses it, and the profile links the farmer and rental features record. */
@AgriScanApiTest
class AccountServiceTest {

    private static final String NEW_PHONE = "9123456780";
    private static final String NEW_PASSWORD = "Tractor2026";

    @Autowired
    private AccountService accountService;

    @Autowired
    private AuthService authService;

    @Autowired
    private TestUsers users;

    @Autowired
    private Messages messages;

    @Test
    void createRegistersAnActiveAccountWithANormalisedPhoneNumber() {
        Account created = accountService.create(new NewAccount(" Ganesh More ", "+91 91234-56780", "  ",
                Set.of(Role.FARMER, Role.VEHICLE_OWNER), NEW_PASSWORD, Language.MR));

        assertThat(created.phone()).isEqualTo(NEW_PHONE);
        assertThat(created.name()).isEqualTo("Ganesh More");
        assertThat(created.email()).isNull();
        assertThat(created.roles()).containsExactly(Role.FARMER, Role.VEHICLE_OWNER);
        assertThat(created.active()).isTrue();
        assertThat(created.preferredLanguage()).isEqualTo(Language.MR);
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(created.lastLoginAt()).isNull();
        assertThat(created.farmerId()).isNull();
        assertThat(authService.login(NEW_PHONE, NEW_PASSWORD).response().user().id()).isEqualTo(created.id());
    }

    @Test
    void aPhoneNumberCanBeRegisteredOnlyOnce() {
        assertThatThrownBy(() -> accountService.create(newFarmer("+91 98765 43210")))
                .isInstanceOf(ConflictException.class)
                .satisfies(error -> assertThat(englishMessageOf(error))
                        .isEqualTo("The phone number 9876543210 is already registered"));
    }

    @Test
    void createValidatesTheNewAccount() {
        assertThatThrownBy(() -> accountService.create(newFarmer("5123456780"))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> accountService.create(new NewAccount(" ", NEW_PHONE, null, Set.of(Role.FARMER),
                NEW_PASSWORD, Language.EN))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> accountService.create(new NewAccount("Ganesh More", NEW_PHONE, null, Set.of(),
                NEW_PASSWORD, Language.EN))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> accountService.create(new NewAccount("Ganesh More", NEW_PHONE, null, Set.of(Role.FARMER),
                "password", Language.EN))).isInstanceOf(BadRequestException.class);
    }

    @Test
    void adminUpdatesAnotherAccount() {
        Account owner = users.account(OWNER_PHONE);

        Account updated = accountService.update(adminId(), owner.id(), new AccountUpdate("Rameshwar B. Patil",
                new Change<>("rameshwar@example.com"), Set.of(Role.VEHICLE_OWNER, Role.FARMER), false, Language.HI));

        assertThat(updated.name()).isEqualTo("Rameshwar B. Patil");
        assertThat(updated.email()).isEqualTo("rameshwar@example.com");
        assertThat(updated.roles()).containsExactly(Role.FARMER, Role.VEHICLE_OWNER);
        assertThat(updated.active()).isFalse();
        assertThat(updated.preferredLanguage()).isEqualTo(Language.HI);
    }

    @Test
    void fieldsLeftOutOfAnUpdateStayAsTheyWere() {
        Account farmer = users.account(DEMO_FARMER_PHONE);

        Account updated = accountService.update(adminId(), farmer.id(), new AccountUpdate(null, null, null, null, null));

        assertThat(updated).isEqualTo(farmer);
    }

    @Test
    void adminsCannotLockThemselvesOut() {
        long adminId = adminId();

        assertThatThrownBy(() -> accountService.update(adminId, adminId, new AccountUpdate(null, null, null, false, null)))
                .isInstanceOf(ConflictException.class)
                .satisfies(error -> assertThat(englishMessageOf(error)).isEqualTo("You cannot deactivate your own account"));
        assertThatThrownBy(() -> accountService.update(adminId, adminId,
                new AccountUpdate(null, null, Set.of(Role.FARMER), null, null)))
                .isInstanceOf(ConflictException.class)
                .satisfies(error -> assertThat(englishMessageOf(error)).isEqualTo("You cannot remove your own ADMIN role"));
        assertThat(accountService.update(adminId, adminId,
                new AccountUpdate("Chief Admin", null, Set.of(Role.ADMIN, Role.FARMER), true, null)).roles())
                .containsExactly(Role.FARMER, Role.ADMIN);
    }

    @Test
    void anUpdateNeedsAtLeastOneRole() {
        long ownerId = users.account(OWNER_PHONE).id();

        assertThatThrownBy(() -> accountService.update(adminId(), ownerId, new AccountUpdate(null, null, Set.of(), null, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resetPasswordReplacesThePasswordAndFollowsThePolicy() {
        long farmerId = users.account(SECOND_FARMER_PHONE).id();

        accountService.resetPassword(farmerId, NEW_PASSWORD);

        assertThat(authService.login(SECOND_FARMER_PHONE, NEW_PASSWORD).response().user().id()).isEqualTo(farmerId);
        assertThatThrownBy(() -> accountService.resetPassword(farmerId, "short1")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> accountService.resetPassword(999, NEW_PASSWORD)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void profilesAreLinkedOnceAndRelinkingTheSameProfileIsHarmless() {
        long adminId = adminId();
        long seededOwnerProfile = users.account(OWNER_PHONE).ownerId();

        assertThat(accountService.linkOwnerProfile(adminId, 41).ownerId()).isEqualTo(41);
        assertThat(accountService.linkOwnerProfile(adminId, 41).ownerId()).isEqualTo(41);
        assertThatIllegalStateException().isThrownBy(() -> accountService.linkOwnerProfile(adminId, 42));
        assertThat(accountService.linkFarmerProfile(adminId, 7).farmerId()).isEqualTo(7);
        assertThatIllegalStateException().isThrownBy(() -> accountService.linkFarmerProfile(adminId, 8));
        assertThat(accountService.linkOwnerProfile(users.account(OWNER_PHONE).id(), seededOwnerProfile).ownerId())
                .isEqualTo(seededOwnerProfile);
    }

    @Test
    void accountsAreFoundByIdOrByPhoneInAnyCommonFormat() {
        Account farmer = users.account(DEMO_FARMER_PHONE);

        assertThat(accountService.get(farmer.id())).isEqualTo(farmer);
        assertThat(accountService.findByPhone("+91 98765-43210")).contains(farmer);
        assertThat(accountService.findByPhone("9123456780")).isEmpty();
        assertThat(accountService.findByPhone("not a phone")).isEmpty();
        assertThatThrownBy(() -> accountService.get(999)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void searchMatchesNameOrPhoneAndFiltersByRole() {
        assertThat(names(accountService.search("sun", null, 0, 20))).containsExactly("Sunita Pawar");
        assertThat(names(accountService.search("98000", null, 0, 20)))
                .containsExactly("Mahesh Jadhav", "Rameshwar Patil", "Sanjay Kulkarni", "Shivraj Agro Service");
        assertThat(names(accountService.search(null, Role.SHOPKEEPER, 0, 20))).containsExactly("Mahesh Jadhav",
                "Sanjay Kulkarni");
        assertThat(names(accountService.search(null, Role.FARMER, 0, 20))).containsExactly("Rishikesh", "Sunita Pawar");
        assertThat(names(accountService.search("  ", Role.VEHICLE_OWNER, 0, 20))).containsExactly("AgriScan Logistics",
                "Balwant Transport Fleet", "Ganesh Kale", "Lasalgaon Onion Carriers", "Nitin Aher", "Pawar Transport",
                "Rameshwar Patil", "Shivraj Agro Service", "Vinod Shinde");
        assertThat(names(accountService.search("%", null, 0, 20))).isEmpty();
    }

    @Test
    void searchPagesThroughTheResults() {
        Page<Account> secondPage = accountService.search(null, Role.VEHICLE_OWNER, 1, 2);

        assertThat(names(secondPage)).containsExactly("Ganesh Kale", "Lasalgaon Onion Carriers");
        assertThat(secondPage.getTotalElements()).isEqualTo(9);
        assertThat(secondPage.getTotalPages()).isEqualTo(5);
        assertThat(accountService.search(null, null, 0, 1000).getSize()).isEqualTo(AccountService.MAX_PAGE_SIZE);
    }

    @Test
    void countsAccountsPerRole() {
        assertThat(accountService.countByRole())
                .isEqualTo(Map.of(Role.FARMER, 2L, Role.VEHICLE_OWNER, 9L, Role.SHOPKEEPER, 2L, Role.ADMIN, 1L));
    }

    private long adminId() {
        return users.account(ADMIN_PHONE).id();
    }

    private String englishMessageOf(Throwable error) {
        return ((ApiException) error).userMessage(messages, Language.EN);
    }

    private static NewAccount newFarmer(String phone) {
        return new NewAccount("Ganesh More", phone, null, Set.of(Role.FARMER), NEW_PASSWORD, Language.EN);
    }

    private static List<String> names(Page<Account> page) {
        return page.map(Account::name).getContent();
    }
}
