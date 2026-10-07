package com.myagree.app.seed;

import java.time.Clock;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.myagree.app.common.i18n.LocalizedText;
import com.myagree.app.store.RxBundle;
import com.myagree.app.store.RxBundleRepository;

/** Rishikesh's "Doctor's Rx" bundle in the store, prescribed after his Early Blight scan. */
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
        rxBundleRepository.save(new RxBundle(context.get(FarmerSeed.FARMERS).rishikesh().getId(),
                context.get(StoreSeed.CATALOG).rxBundle(), context.get(ScanSeed.SCANS).earlyBlight().getId(),
                LocalizedText.of("Plot A • Blight Triage Rx", "प्लॉट A • करपा उपचार", "प्लॉट A • झुलसा उपचार"),
                clock.instant().minus(RX_PRESCRIBED_AGO),
                LocalizedText.of("100% Genuine Lab-Tested", "100% अस्सल, प्रयोगशाळेत तपासलेले", "100% असली, लैब में जाँचा"),
                LocalizedText.of("DBT Subsidy Eligible", "DBT अनुदानास पात्र", "DBT सब्सिडी के योग्य")));
    }
}
