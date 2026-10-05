package com.myagree.app.seed;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

import com.myagree.app.plot.Crop;
import com.myagree.app.plot.Plot;
import com.myagree.app.plot.PlotLink;
import com.myagree.app.scan.Scan;
import com.myagree.app.scan.ScanImage;
import com.myagree.app.scan.ScanRepository;
import com.myagree.app.scan.diagnosis.Diagnosis;
import com.myagree.app.scan.diagnosis.DiseaseKnowledgeBase;
import com.myagree.app.scan.diagnosis.ScanMode;
import com.myagree.app.seed.PlotSeed.FieldPlots;

/** The two scans on the home screen, built from the same knowledge base the demo diagnosis engine uses. */
@Component
public class ScanSeed implements DemoSeed {

    /** Rishikesh's scans: the Early Blight on plot A and the healthy cotton on plot C. */
    public record DemoScans(Scan earlyBlight, Scan healthyCotton) {
    }

    public static final SeedKey<DemoScans> SCANS = SeedKey.named("demo scans");

    /** "Scanned Yesterday 04:30 PM" on the home screen. */
    private static final LocalTime EARLY_BLIGHT_SCAN_TIME = LocalTime.of(16, 30);
    private static final Duration COTTON_SCAN_AGE = Duration.ofDays(3);

    private final ScanRepository scanRepository;
    private final DiseaseKnowledgeBase knowledgeBase;
    private final Clock clock;

    ScanSeed(ScanRepository scanRepository, DiseaseKnowledgeBase knowledgeBase, Clock clock) {
        this.scanRepository = scanRepository;
        this.knowledgeBase = knowledgeBase;
        this.clock = clock;
    }

    @Override
    public int order() {
        return 50;
    }

    /** Seeds both scans, files them under their plots and makes them each plot's latest diagnosis. */
    @Override
    public void seed(SeedContext context) {
        FieldPlots plots = context.get(PlotSeed.PLOTS);
        int localStockists = context.get(CareSeed.STOCKISTS).size();
        Scan earlyBlight = scanRepository.save(leafScan(Crop.TOMATO, yesterdayAt(EARLY_BLIGHT_SCAN_TIME),
                "/images/scans/early-blight-hero.jpg", localStockists, plots.tomato()));
        Scan healthyCotton = scanRepository.save(leafScan(Crop.COTTON, clock.instant().minus(COTTON_SCAN_AGE),
                "/images/scans/healthy-cotton-leaf.jpg", 0, plots.cotton()));
        plots.tomato().recordLatestScan(earlyBlight.getId());
        plots.cotton().recordLatestScan(healthyCotton.getId());
        context.put(SCANS, new DemoScans(earlyBlight, healthyCotton));
    }

    private Scan leafScan(Crop crop, Instant scannedAt, String photoUrl, int stockistCount, Plot plot) {
        Diagnosis diagnosis = new Diagnosis(crop, knowledgeBase.profileFor(crop, ScanMode.LEAF));
        Scan scan = new Scan(diagnosis, ScanMode.LEAF, scannedAt, ScanImage.staticAsset(photoUrl), stockistCount);
        scan.linkToPlot(new PlotLink(plot.getId(), plot.label(), null));
        return scan;
    }

    private Instant yesterdayAt(LocalTime time) {
        return LocalDate.now(clock).minusDays(1).atTime(time).atZone(clock.getZone()).toInstant();
    }
}
