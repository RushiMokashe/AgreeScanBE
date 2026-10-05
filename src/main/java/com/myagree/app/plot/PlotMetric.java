package com.myagree.app.plot;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.myagree.app.common.Tone;

/** One stat tile on a plot card, e.g. "Harvest Cycle: 12 days left". */
@Embeddable
public record PlotMetric(
        String label,
        @Column(name = "metric_value") String value,
        @Enumerated(EnumType.STRING) Tone tone) {
}
