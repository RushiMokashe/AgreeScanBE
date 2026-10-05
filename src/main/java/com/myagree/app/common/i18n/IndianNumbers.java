package com.myagree.app.common.i18n;

import java.util.regex.Pattern;

/**
 * Whole numbers as India writes them, in every language AgriScan speaks: Latin digits, the last three grouped and the
 * rest in pairs (lakh, crore), e.g. "1,08,230", as the frontend formats them. The JDK's number formats, and so
 * {@link Messages} placeholders given a number, group in threes only ("108,230"): pass amounts to a message already
 * formatted, e.g. {@code messages.get("store.order.placed", language, String.valueOf(orderId), rupees(total))}.
 */
public final class IndianNumbers {

    private static final String RUPEE_SIGN = "₹";
    private static final String MINUS_SIGN = "-";
    private static final String SEPARATOR = ",";
    /** Digits after the last separator; the ones before them are grouped in pairs. */
    private static final int LAST_GROUP_DIGITS = 3;
    /** The positions inside the leading digits where a lakh or crore separator goes. */
    private static final Pattern PAIR_BOUNDARY = Pattern.compile("\\B(?=(\\d{2})+$)");

    private IndianNumbers() {
    }

    /** 8450 gives "8,450", 108230 gives "1,08,230" and -1234567 gives "-12,34,567". */
    public static String format(long value) {
        String digits = Long.toString(value);
        String sign = value < 0 ? MINUS_SIGN : "";
        String magnitude = digits.substring(sign.length());
        if (magnitude.length() <= LAST_GROUP_DIGITS) {
            return digits;
        }
        int split = magnitude.length() - LAST_GROUP_DIGITS;
        return sign + PAIR_BOUNDARY.matcher(magnitude.substring(0, split)).replaceAll(SEPARATOR)
                + SEPARATOR + magnitude.substring(split);
    }

    /** 1850 gives "₹1,850"; a negative amount, such as a refund, gives "-₹500". */
    public static String rupees(long amount) {
        String grouped = format(amount);
        return amount < 0 ? MINUS_SIGN + RUPEE_SIGN + grouped.substring(MINUS_SIGN.length()) : RUPEE_SIGN + grouped;
    }
}
