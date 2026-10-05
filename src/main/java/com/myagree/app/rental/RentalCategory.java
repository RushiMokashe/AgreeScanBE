package com.myagree.app.rental;

/**
 * What kind of vehicle a listing offers; mirrors {@code RentalCategory} in frontend/src/lib/types.ts. Each category
 * knows the Material Symbols it shows when an owner has not chosen one.
 */
public enum RentalCategory {

    /** Tractors and implements that work the field. */
    MACHINERY("agriculture", "calendar_month"),

    /** Tempos and trucks that carry the harvest to the mandi. */
    TRANSPORT("local_shipping", "near_me");

    private final String defaultIcon;
    private final String defaultBookIcon;

    RentalCategory(String defaultIcon, String defaultBookIcon) {
        this.defaultIcon = defaultIcon;
        this.defaultBookIcon = defaultBookIcon;
    }

    /** The tile icon of a vehicle listed without one of its own. */
    public String defaultIcon() {
        return defaultIcon;
    }

    /** The icon on the book button of a vehicle AgriScan has not given a button of its own. */
    public String defaultBookIcon() {
        return defaultBookIcon;
    }
}
