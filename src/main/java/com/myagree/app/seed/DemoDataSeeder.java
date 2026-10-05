package com.myagree.app.seed;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.account.AccountService;

/**
 * Loads the demo data from the Stitch designs into an empty database at startup by running every {@link DemoSeed}
 * bean in order, in one transaction. Timestamps are relative to the injected clock, so labels such as "Scanned
 * Yesterday 04:30 PM" and "Updated 18 min ago" match the designs on any day.
 */
@Component
class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AccountService accountService;
    private final List<DemoSeed> seeds;

    DemoDataSeeder(AccountService accountService, List<DemoSeed> seeds) {
        this.accountService = accountService;
        this.seeds = inRunOrder(seeds);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accountService.hasAccounts()) {
            log.info("Demo data already present; skipping seed");
            return;
        }
        SeedContext context = new SeedContext();
        seeds.forEach(seed -> seed.seed(context));
        log.info("Seeded AgriScan demo data: {}", seeds.stream()
                .map(seed -> seed.getClass().getSimpleName())
                .collect(Collectors.joining(", ")));
    }

    /**
     * @throws IllegalStateException when two seeds share an order, which would leave their run order to chance
     */
    static List<DemoSeed> inRunOrder(List<DemoSeed> seeds) {
        List<DemoSeed> ordered = seeds.stream().sorted(Comparator.comparingInt(DemoSeed::order)).toList();
        for (int i = 1; i < ordered.size(); i++) {
            DemoSeed previous = ordered.get(i - 1);
            DemoSeed current = ordered.get(i);
            if (previous.order() == current.order()) {
                throw new IllegalStateException("Demo seeds %s and %s share order %d".formatted(
                        previous.getClass().getSimpleName(), current.getClass().getSimpleName(), current.order()));
            }
        }
        return ordered;
    }
}
