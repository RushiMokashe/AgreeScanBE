package com.myagree.app.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;

import org.junit.jupiter.api.Test;

class DemoDataSeederTest {

    @Test
    void seedsRunLowestOrderFirst() {
        DemoSeed accounts = new NamedSeed("accounts", 0);
        DemoSeed plots = new NamedSeed("plots", 40);
        DemoSeed ownerProfiles = new NamedSeed("owner profiles", 45);

        assertThat(DemoDataSeeder.inRunOrder(List.of(ownerProfiles, plots, accounts)))
                .containsExactly(accounts, plots, ownerProfiles);
    }

    @Test
    void twoSeedsWithTheSameOrderAreRefused() {
        List<DemoSeed> seeds = List.of(new NamedSeed("plots", 40), new NamedSeed("farm ponds", 40));

        assertThatIllegalStateException().isThrownBy(() -> DemoDataSeeder.inRunOrder(seeds))
                .withMessageContaining("share order 40");
    }

    private record NamedSeed(String name, int order) implements DemoSeed {

        @Override
        public void seed(SeedContext context) {
        }
    }
}
