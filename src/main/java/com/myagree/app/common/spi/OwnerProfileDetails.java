package com.myagree.app.common.spi;

import org.jspecify.annotations.Nullable;

/**
 * A new vehicle-owner profile.
 *
 * @param businessName the owner's business, e.g. "Shivraj Agro Service"; {@code null} for none
 * @param hubId        the rental hub the owner's vehicles serve
 */
public record OwnerProfileDetails(@Nullable String businessName, long hubId) implements ProfileDetails {
}
