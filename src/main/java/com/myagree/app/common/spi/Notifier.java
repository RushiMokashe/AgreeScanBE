package com.myagree.app.common.spi;

import java.util.Map;

/**
 * Tells a user that one of their bookings or payments changed (docs/architecture/phase-2.md, D10).
 *
 * <p><b>Implemented by</b> the notification slice: it stores the notification (type, parameters, route), renders its
 * title and body in the reader's language whenever it is read, and pushes it to the reader's open event streams.
 * <b>Consumed by</b> the rental slice (the booking lifecycle) and the payment slice (payment results), through
 * {@code ObjectProvider<Notifier>}: {@code notifiers.ifAvailable(notifier -> notifier.notify(...))}, so bookings and
 * payments work before the notification slice exists.
 *
 * <p><b>Contract:</b> call it inside the transaction that made the change. The notification is stored in that
 * transaction and pushed only after it commits, so a rolled-back change notifies nobody. Delivery problems never
 * fail the caller (the reader sees the notification the next time the list loads). An
 * {@link IllegalArgumentException} reports a programming error: a parameter of {@link NotificationType#parameters()}
 * missing from {@code params}.
 *
 * <p>Parameter values are display text, stored with the notification: names as entered, free text as typed,
 * translatable text (listing names, payment descriptions) in the recipient's preferred language, and amounts as
 * whole rupees in plain digits, which the notification slice formats for the reader ("1300" shows as "₹1,300").
 */
public interface Notifier {

    /** Parameter: the farmer's name, e.g. "Rishikesh". */
    String FARMER_NAME = "farmerName";
    /** Parameter: the owner's name as farmers see it, e.g. "Rameshwar Patil". */
    String OWNER_NAME = "ownerName";
    /** Parameter: the vehicle's name, e.g. "Mahindra 575 DI (45 HP) + Rotavator". */
    String LISTING_NAME = "listingName";
    /** Parameter: the booked slot as the farmer chose it, e.g. "Slot: 02:00 PM Today". */
    String SLOT = "slot";
    /** Parameter: whole rupees in plain digits, e.g. "1300". */
    String AMOUNT = "amount";
    /** Parameter: a decline, cancel or failure reason as typed or reported; empty when none was given. */
    String REASON = "reason";
    /** Parameter: what a payment was for, e.g. "Agro Store order #12". */
    String DESCRIPTION = "description";
    /** Parameter: a 12-digit UPI transaction reference (UTR), e.g. "412345678901". */
    String UPI_REFERENCE = "upiReference";
    /** Parameter: who a Scan & Pay payment went to, e.g. "Solapur Mandi Agro Depot". */
    String PAYEE_NAME = "payeeName";

    /**
     * @param userId the recipient's account id ({@code Account.id()}, not a farmer or owner profile id)
     * @param type   what happened; decides the texts and the parameters they need
     * @param params a value for every key in {@code type.parameters()}
     * @param route  the app screen with the details, e.g. "/owner/bookings/7" or "/bookings/7"
     */
    void notify(long userId, NotificationType type, Map<String, String> params, String route);
}
