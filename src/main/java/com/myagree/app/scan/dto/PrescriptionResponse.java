package com.myagree.app.scan.dto;

/** Mirrors {@code Prescription} in frontend/src/lib/types.ts. */
public record PrescriptionResponse(
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
