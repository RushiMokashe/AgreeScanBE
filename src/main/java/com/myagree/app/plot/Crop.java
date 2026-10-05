package com.myagree.app.plot;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Crops AgriScan grows and diagnoses, in the order the scanner offers them. */
public enum Crop {
    TOMATO("Tomato", "🍅", "Solanum lycopersicum"),
    WHEAT("Wheat", "🌾", "Triticum aestivum"),
    PADDY("Rice / Paddy", "🌱", "Oryza sativa"),
    COTTON("Cotton", "☁️", "Gossypium hirsutum"),
    CHILLI("Chilli", "🌶️", "Capsicum annuum"),
    SUGARCANE("Sugarcane", "🎋", "Saccharum officinarum");

    private final String label;
    private final String emoji;
    private final String scientificName;

    Crop(String label, String emoji, String scientificName) {
        this.label = label;
        this.emoji = emoji;
        this.scientificName = scientificName;
    }

    /** Looks a crop up by its code, ignoring case and surrounding whitespace. */
    public static Optional<Crop> fromCode(String code) {
        String normalized = code.strip().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(crop -> crop.name().equals(normalized))
                .findFirst();
    }

    public String label() {
        return label;
    }

    public String emoji() {
        return emoji;
    }

    public String scientificName() {
        return scientificName;
    }
}
