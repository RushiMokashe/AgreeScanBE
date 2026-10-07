package com.myagree.app.common;

/** A UPI ID ("virtual payment address") as UPI apps show it, e.g. "solapur.agro@okaxis": where Scan & Pay sends money. */
public final class UpiId {

    public static final int MAX_LENGTH = 100;
    /** "name@bank"; the empty string is allowed too, for a form that clears the UPI ID. */
    public static final String PATTERN = "^$|^[\\w.-]{2,256}@[a-zA-Z][a-zA-Z0-9]{1,64}$";

    private UpiId() {
    }
}
