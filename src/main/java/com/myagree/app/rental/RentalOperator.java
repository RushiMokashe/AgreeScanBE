package com.myagree.app.rental;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The person or fleet who drives the rented machine.
 *
 * @param stats track record, e.g. "120+ acres tilled • 4.9 ★★★★★"
 */
@Embeddable
public record RentalOperator(
        @Column(name = "operator_name") String name,
        @Column(name = "operator_initials") String initials,
        @Column(name = "operator_stats") String stats) {
}
