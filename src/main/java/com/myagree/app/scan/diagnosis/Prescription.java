package com.myagree.app.scan.diagnosis;

import jakarta.persistence.Embeddable;

/**
 * The chemical treatment for a diagnosis, including dosage for a standard knapsack sprayer.
 *
 * @param preHarvestNote may contain **bold** markers
 */
@Embeddable
public record Prescription(
        String category,
        boolean recommended,
        String activeIngredient,
        String marketBrands,
        String dilution,
        String dilutionNote,
        String tankSize,
        String tankDose,
        String tankNote,
        String applicationWindow,
        int preHarvestIntervalDays,
        String preHarvestNote) {
}
