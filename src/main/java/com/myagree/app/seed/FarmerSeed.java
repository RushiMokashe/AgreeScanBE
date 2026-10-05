package com.myagree.app.seed;

import org.springframework.stereotype.Component;

import com.myagree.app.account.Account;
import com.myagree.app.account.AccountService;
import com.myagree.app.farmer.Farmer;
import com.myagree.app.farmer.FarmerRepository;
import com.myagree.app.seed.AccountSeed.DemoAccounts;

/** The farmer profiles of the two demo farmer accounts, each linked to its account. */
@Component
public class FarmerSeed implements DemoSeed {

    /** Rishikesh owns all phase-1 demo data; Sunita Pawar starts with an empty farm. */
    public record DemoFarmers(Farmer rishikesh, Farmer sunitaPawar) {
    }

    public static final SeedKey<DemoFarmers> FARMERS = SeedKey.named("demo farmers");

    private static final String SEASON = "Rabi 2024";
    /** Sunita has no profile photo yet; the brand mark stands in. */
    private static final String PLACEHOLDER_AVATAR_URL = "/images/brand/agriscan-mark.svg";

    private final FarmerRepository farmerRepository;
    private final AccountService accountService;

    FarmerSeed(FarmerRepository farmerRepository, AccountService accountService) {
        this.farmerRepository = farmerRepository;
        this.accountService = accountService;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public void seed(SeedContext context) {
        DemoAccounts accounts = context.get(AccountSeed.ACCOUNTS);
        context.put(FARMERS, new DemoFarmers(
                createProfile(accounts.rishikesh(), "Solapur, Maharashtra", "/images/people/farmer-profile.jpg"),
                createProfile(accounts.sunitaPawar(), "Karmala, Maharashtra", PLACEHOLDER_AVATAR_URL)));
    }

    private Farmer createProfile(Account account, String location, String avatarUrl) {
        Farmer farmer = farmerRepository.save(new Farmer(account.id(), account.name(), location, SEASON, avatarUrl));
        accountService.linkFarmerProfile(account.id(), farmer.getId());
        return farmer;
    }
}
