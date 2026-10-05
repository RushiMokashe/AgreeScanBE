package com.myagree.app.weather;

/**
 * Field readings that decide how urgent a crop scan is.
 *
 * @param moisturePercent     leaf and topsoil moisture
 * @param moistureNote        agronomic reading of that moisture, e.g. "Optimal for Fungal Check"
 * @param nearbyOutbreakCount disease outbreaks reported around the farm
 * @param nearbyOutbreakNote  what and where, e.g. "Blight within 5 km"
 */
public record FieldConditions(
        int moisturePercent,
        String moistureNote,
        int nearbyOutbreakCount,
        String nearbyOutbreakNote) {
}
