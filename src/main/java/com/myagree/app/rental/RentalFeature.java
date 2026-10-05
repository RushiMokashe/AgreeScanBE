package com.myagree.app.rental;

import jakarta.persistence.Embeddable;

/** A service feature chip with a Material Symbols icon, e.g. "Solapur APMC Direct Entry Pass". */
@Embeddable
public record RentalFeature(String icon, String text) {
}
