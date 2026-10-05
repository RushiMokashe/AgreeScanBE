package com.myagree.app.store;

/** EAN-13 barcodes as printed on product packs: 13 digits, the last one a check digit over the first twelve. */
final class Ean13 {

    static final int LENGTH = 13;

    private static final int RADIX = 10;
    /** Every second digit, starting with the second, counts three times towards the check digit. */
    private static final int ALTERNATE_DIGIT_WEIGHT = 3;

    private Ean13() {
    }

    /** Whether {@code code} is 13 ASCII digits ending in the check digit of the first twelve, e.g. "8904567001017". */
    static boolean isValid(String code) {
        if (code.length() != LENGTH || !code.chars().allMatch(Ean13::isAsciiDigit)) {
            return false;
        }
        int weightedSum = 0;
        for (int position = 0; position < LENGTH - 1; position++) {
            int digit = code.charAt(position) - '0';
            weightedSum += position % 2 == 0 ? digit : digit * ALTERNATE_DIGIT_WEIGHT;
        }
        int checkDigit = (RADIX - weightedSum % RADIX) % RADIX;
        return code.charAt(LENGTH - 1) - '0' == checkDigit;
    }

    private static boolean isAsciiDigit(int character) {
        return character >= '0' && character <= '9';
    }
}
