package com.myagree.app.rental;

import jakarta.persistence.Embeddable;

import org.hibernate.annotations.EmbeddedColumnNaming;

import com.myagree.app.common.i18n.LocalizedText;

/**
 * A service feature chip, e.g. "Solapur APMC Direct Entry Pass".
 *
 * @param icon a Material Symbols name, e.g. "badge"
 */
@Embeddable
public record RentalFeature(String icon, @EmbeddedColumnNaming("text_%s") LocalizedText text) {
}
