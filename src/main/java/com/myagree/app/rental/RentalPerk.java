package com.myagree.app.rental;

import jakarta.persistence.Embeddable;

import org.hibernate.annotations.EmbeddedColumnNaming;

import com.myagree.app.common.i18n.LocalizedText;

/**
 * A trust badge on the spotlight offer, e.g. "Zero Spill" / "Tarpaulin Tied".
 *
 * @param icon a Material Symbols name, e.g. "verified_user"
 */
@Embeddable
public record RentalPerk(
        String icon,
        @EmbeddedColumnNaming("title_%s") LocalizedText title,
        @EmbeddedColumnNaming("subtitle_%s") LocalizedText subtitle) {
}
