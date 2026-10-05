package com.myagree.app.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;

import org.junit.jupiter.api.Test;

class SeedContextTest {

    private static final SeedKey<List<String>> HUBS = SeedKey.named("rental hubs");

    private final SeedContext context = new SeedContext();

    @Test
    void aRecordPutUnderAKeyIsReadBackWithItsType() {
        context.put(HUBS, List.of("Solapur APMC Hub"));

        List<String> hubs = context.get(HUBS);

        assertThat(hubs).containsExactly("Solapur APMC Hub");
    }

    @Test
    void keysCompareByIdentityNotByName() {
        context.put(HUBS, List.of("Solapur APMC Hub"));

        assertThatIllegalStateException().isThrownBy(() -> context.get(SeedKey.<List<String>>named("rental hubs")));
    }

    @Test
    void readingARecordNoSeedHasPutYetExplainsTheOrder() {
        assertThatIllegalStateException().isThrownBy(() -> context.get(HUBS))
                .withMessageContaining("rental hubs")
                .withMessageContaining("higher order()");
    }

    @Test
    void aRecordCannotBePutTwice() {
        context.put(HUBS, List.of("Solapur APMC Hub"));

        assertThatIllegalStateException().isThrownBy(() -> context.put(HUBS, List.of("Pune Hub")));
    }
}
