package com.myagree.app.rental;

import jakarta.persistence.Embeddable;

import org.hibernate.annotations.EmbeddedColumnNaming;

import com.myagree.app.common.i18n.LocalizedText;

/** A machine spec tile, e.g. "6 Feet" / "Rotavator Width". */
@Embeddable
public record RentalSpec(
        @EmbeddedColumnNaming("value_%s") LocalizedText value,
        @EmbeddedColumnNaming("label_%s") LocalizedText label) {
}
