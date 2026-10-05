package com.myagree.app.seed;

import java.util.List;

import org.springframework.stereotype.Component;

import com.myagree.app.common.Tone;
import com.myagree.app.plot.Crop;
import com.myagree.app.plot.Plot;
import com.myagree.app.plot.PlotHealth;
import com.myagree.app.plot.PlotMetric;
import com.myagree.app.plot.PlotRepository;

/** The demo farm's three plots, as shown under "My Field Plots". */
@Component
public class PlotSeed implements DemoSeed {

    /** Rishikesh's plots A, B and C. */
    public record FieldPlots(Plot tomato, Plot wheat, Plot cotton) {
    }

    public static final SeedKey<FieldPlots> PLOTS = SeedKey.named("field plots");

    private final PlotRepository plotRepository;

    PlotSeed(PlotRepository plotRepository) {
        this.plotRepository = plotRepository;
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public void seed(SeedContext context) {
        Plot tomato = plotRepository.save(new Plot(
                "Plot A", "North Field", Crop.TOMATO, "Arka Rakshak", 2.5, "/images/plots/tomato.jpg",
                PlotHealth.ACTION_NEEDED,
                List.of(new PlotMetric("Harvest Cycle", "12 days left", Tone.NEUTRAL),
                        new PlotMetric("Last Diagnostic", "Yesterday (Blight)", Tone.ERROR)),
                "Fungicide spray pending"));
        Plot wheat = plotRepository.save(new Plot(
                "Plot B", "South Field", Crop.WHEAT, "HD-2967", 4.0, "/images/plots/wheat.jpg",
                PlotHealth.HEALTHY,
                List.of(new PlotMetric("Crop Age", "45 Days", Tone.NEUTRAL),
                        new PlotMetric("Soil Moisture", "Optimal (24%)", Tone.SUCCESS)),
                null));
        Plot cotton = plotRepository.save(new Plot(
                "Plot C", "East Field", Crop.COTTON, "Bt Cotton", 1.8, "/images/plots/cotton.jpg",
                PlotHealth.MODERATE_RISK,
                List.of(new PlotMetric("Last Treatment", "2d ago (Neem)", Tone.NEUTRAL),
                        new PlotMetric("Pest Vector", "Whitefly (Low)", Tone.NEUTRAL)),
                null));
        context.put(PLOTS, new FieldPlots(tomato, wheat, cotton));
    }
}
