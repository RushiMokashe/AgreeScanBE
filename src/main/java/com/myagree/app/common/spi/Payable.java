package com.myagree.app.common.spi;

/**
 * An order or booking the signed-in farmer can pay online, as its owning slice describes it to the payment slice.
 * The server computes every amount; a client never sends one.
 *
 * @param purpose      what is paid for
 * @param referenceId  the order or booking id
 * @param farmerId     the farmer who pays: always the farmer the resolver was asked about
 * @param amountRupees the amount due in whole rupees, at least 1
 * @param description  what the payment is for, in the request's language, e.g. "Agro Store order #12"; shown at
 *                     checkout and sent to the payment provider
 * @param payerName    the farmer's name, prefilled in the provider's checkout
 * @param payerPhone   the farmer's 10-digit mobile number, prefilled in the provider's checkout
 */
public record Payable(
        PaymentPurpose purpose,
        long referenceId,
        long farmerId,
        long amountRupees,
        String description,
        String payerName,
        String payerPhone) {

    public Payable {
        if (amountRupees < 1) {
            throw new IllegalArgumentException("A payable amount is at least 1 rupee, not " + amountRupees);
        }
    }
}
