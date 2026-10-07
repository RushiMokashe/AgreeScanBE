package com.myagree.app.seed;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.myagree.app.account.Account;
import com.myagree.app.account.AccountService;
import com.myagree.app.account.NewAccount;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.Role;

/**
 * The demo accounts of docs/architecture/phase-2.md, section 3. Their passwords are published, so they are for
 * development only. The farmer and rental seeds create the matching profiles and link them.
 */
@Component
public class AccountSeed implements DemoSeed {

    /** The demo accounts as created, before any profile is linked to them. */
    public static final SeedKey<DemoAccounts> ACCOUNTS = SeedKey.named("demo accounts");

    public record DemoAccounts(
            Account admin,
            Account rishikesh,
            Account sunitaPawar,
            Account rameshwarPatil,
            Account shivrajAgroService,
            Account vinodShinde,
            Account balwantTransportFleet,
            Account agriscanLogistics,
            Account sanjayKulkarni,
            Account maheshJadhav) {
    }

    private static final Logger log = LoggerFactory.getLogger(AccountSeed.class);

    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final String FARMER_PASSWORD = "Farmer@123";
    private static final String OWNER_PASSWORD = "Owner@123";
    private static final String SHOPKEEPER_PASSWORD = "Shop@123";

    private final AccountService accountService;

    AccountSeed(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public void seed(SeedContext context) {
        context.put(ACCOUNTS, new DemoAccounts(
                create("AgriScan Admin", "9000000001", Role.ADMIN, ADMIN_PASSWORD),
                create("Rishikesh", "9876543210", Role.FARMER, FARMER_PASSWORD),
                create("Sunita Pawar", "9876543211", Role.FARMER, FARMER_PASSWORD),
                create("Rameshwar Patil", "9800012345", Role.VEHICLE_OWNER, OWNER_PASSWORD),
                create("Shivraj Agro Service", "9800098765", Role.VEHICLE_OWNER, OWNER_PASSWORD),
                create("Vinod Shinde", "9822233445", Role.VEHICLE_OWNER, OWNER_PASSWORD),
                create("Balwant Transport Fleet", "9877766554", Role.VEHICLE_OWNER, OWNER_PASSWORD),
                create("AgriScan Logistics", "9000000009", Role.VEHICLE_OWNER, OWNER_PASSWORD),
                create("Sanjay Kulkarni", "9800054321", Role.SHOPKEEPER, SHOPKEEPER_PASSWORD),
                create("Mahesh Jadhav", "9800054322", Role.SHOPKEEPER, SHOPKEEPER_PASSWORD)));
        log.warn("Seeded the demo accounts; their passwords are public, so never seed a shared or production database");
    }

    private Account create(String name, String phone, Role role, String password) {
        return accountService.create(new NewAccount(name, phone, null, Set.of(role), password, Language.DEFAULT));
    }
}
