package com.myagree.app.seed;

import java.time.Clock;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.myagree.app.store.RxBundle;
import com.myagree.app.store.RxBundleRepository;

/** The store's "Doctor's Rx" bundle, prescribed after the Early Blight scan. */
@Component
class PrescriptionSeed implements DemoSeed {

    private static final Duration RX_PRESCRIBED_AGO = Duration.ofHours(3);

    private final RxBundleRepository rxBundleRepository;
    private final Clock clock;

    PrescriptionSeed(RxBundleRepository rxBundleRepository, Clock clock) {
        this.rxBundleRepository = rxBundleRepository;
        this.clock = clock;
    }

    @Override
    public int order() {
        return 70;
    }

    @Override
    public void seed(SeedContext context) {
        rxBundleRepository.save(new RxBundle(context.get(StoreSeed.CATALOG).rxBundle(),
                context.get(ScanSeed.SCANS).earlyBlight().getId(), "Plot A • Blight Triage Rx",
                clock.instant().minus(RX_PRESCRIBED_AGO), "100% Genuine Lab-Tested", "DBT Subsidy Eligible"));
    }
}
