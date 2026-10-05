package com.myagree.app.scan.diagnosis;

import jakarta.persistence.Embeddable;

/**
 * Residue-free alternative to the chemical prescription.
 *
 * @param summary may contain **bold** markers
 */
@Embeddable
public record OrganicAlternative(String summary, String note, String badge) {
}
