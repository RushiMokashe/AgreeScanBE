package com.myagree.app.mandi;

import jakarta.persistence.Embeddable;

import org.hibernate.annotations.EmbeddedColumnNaming;

import com.myagree.app.common.i18n.LocalizedText;

/**
 * What a {@link CommodityPrice} quotes, in the farmer's language. A board lists each name and grade once.
 *
 * @param category the board's grouping, e.g. "Vegetable"
 * @param name     e.g. "Tomato (Hybrid)"
 * @param grade    e.g. "Grade A (Crates)"
 * @param unit     what the price is per, e.g. "Quintal"
 */
@Embeddable
public record Commodity(
        @EmbeddedColumnNaming("category_%s") LocalizedText category,
        @EmbeddedColumnNaming("name_%s") LocalizedText name,
        @EmbeddedColumnNaming("grade_%s") LocalizedText grade,
        @EmbeddedColumnNaming("unit_%s") LocalizedText unit) {
}
