package com.myagree.app.rental;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** A machine spec tile, e.g. "6 Feet" / "Rotavator Width". */
@Embeddable
public record RentalSpec(@Column(name = "spec_value") String value, String label) {
}
