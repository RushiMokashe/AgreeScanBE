package com.myagree.app.care;

import jakarta.persistence.Embeddable;

/** What a dealer currently stocks for the farmer's prescription, e.g. "Ready Stock Available • ₹280 / 500g". */
@Embeddable
public record DealerStock(String headline, String priceLabel, String detail, String note) {
}
