package com.myagree.app.common.spi;

/**
 * A new farmer profile.
 *
 * @param location the farm's place, e.g. "Karmala, Maharashtra"
 * @param season   the current cropping season, e.g. "Rabi 2024"
 */
public record FarmerProfileDetails(String location, String season) implements ProfileDetails {
}
