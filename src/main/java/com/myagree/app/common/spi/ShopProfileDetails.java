package com.myagree.app.common.spi;

/**
 * A new shop for a shopkeeper account.
 *
 * @param shopName the shop's name farmers see, e.g. "Karmala Krishi Seva Kendra"
 * @param place    its village or town and district, e.g. "Karmala, Solapur"
 */
public record ShopProfileDetails(String shopName, String place) implements ProfileDetails {
}
