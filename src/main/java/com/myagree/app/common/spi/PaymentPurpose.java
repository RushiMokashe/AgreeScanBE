package com.myagree.app.common.spi;

/** What an online payment pays for; mirrors {@code PaymentPurpose} in frontend/src/lib/types.ts. */
public enum PaymentPurpose {

    /** An Agro Store order placed for online payment; the reference is the order id. */
    STORE_ORDER,

    /** A rental booking the owner has accepted; the reference is the booking id. */
    RENTAL_BOOKING
}
