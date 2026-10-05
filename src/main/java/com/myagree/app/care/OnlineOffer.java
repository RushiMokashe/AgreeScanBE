package com.myagree.app.care;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The online depot's alternative to visiting a dealer. The product is referenced by id; it belongs to the
 * store feature, which also owns its price.
 *
 * @param label e.g. "Buy Online from AgriScan Mandi Depot"
 */
@Embeddable
public record OnlineOffer(
        @Column(name = "online_offer_label") String label,
        @Column(name = "online_offer_product_id") long productId) {
}
