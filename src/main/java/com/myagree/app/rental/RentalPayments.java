package com.myagree.app.rental;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.myagree.app.common.ConflictException;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.spi.Payable;
import com.myagree.app.common.spi.PayableResolver;
import com.myagree.app.common.spi.PaymentAwaitingConfirmationEvent;
import com.myagree.app.common.spi.PaymentFailedEvent;
import com.myagree.app.common.spi.PaymentPurpose;
import com.myagree.app.common.spi.PaymentSettledEvent;
import com.myagree.app.common.spi.ScanAndPayPayee;

/**
 * Online payment of bookings (docs/architecture/phase-2.md, D5): describes a booking the farmer may pay to the payment
 * feature, naming its owner as the payee of Scan & Pay, and follows the payment: paid (telling the owner), waiting for
 * the owner to confirm a Scan & Pay payment, or payable again after the owner did not receive one.
 */
@Component
class RentalPayments implements PayableResolver {

    private static final Logger log = LoggerFactory.getLogger(RentalPayments.class);

    private static final UserMessage ALREADY_PAID = UserMessage.of("rental.booking.already-paid");
    private static final UserMessage NOT_PAYABLE = UserMessage.of("rental.booking.not-payable");

    private final RentalBookingRepository bookingRepository;
    private final BookingMapper mapper;
    private final BookingNotifications notifications;
    private final ListingPictures pictures;

    RentalPayments(RentalBookingRepository bookingRepository, BookingMapper mapper,
                   BookingNotifications notifications, ListingPictures pictures) {
        this.bookingRepository = bookingRepository;
        this.mapper = mapper;
        this.notifications = notifications;
        this.pictures = pictures;
    }

    @Override
    public PaymentPurpose purpose() {
        return PaymentPurpose.RENTAL_BOOKING;
    }

    /**
     * A booking is payable once the owner accepted it, until it is paid; the amount is the one fixed when booking. A
     * booking whose Scan & Pay payment waits for the owner is resolved too, so the payment feature can say so.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Payable> resolvePayable(long referenceId, long farmerId, Language language) {
        return bookingRepository.findOneForFarmer(referenceId, farmerId).map(booking -> {
            if (booking.getPaymentStatus() == BookingPaymentStatus.PAID) {
                throw new ConflictException(ALREADY_PAID);
            }
            if (!booking.isPayable() && booking.getPaymentStatus() != BookingPaymentStatus.VERIFYING) {
                throw new ConflictException(NOT_PAYABLE);
            }
            BookingFarmer farmer = booking.getFarmer();
            return new Payable(PaymentPurpose.RENTAL_BOOKING, booking.getId(), farmerId, booking.getAmount(),
                    mapper.paymentDescription(booking, language), farmer.name(), farmer.phone(), payee(booking));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> describe(long referenceId, Language language) {
        return bookingRepository.findWithListingById(referenceId)
                .map(booking -> mapper.paymentDescription(booking, language));
    }

    /** The farmer paid the owner by Scan & Pay: the booking waits for the owner, who is told to check their bank. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(PaymentAwaitingConfirmationEvent event) {
        if (event.purpose() != PaymentPurpose.RENTAL_BOOKING) {
            return;
        }
        bookingRepository.findWithListingById(event.referenceId())
                .filter(RentalBooking::awaitPaymentConfirmation)
                .ifPresent(booking -> notifications.paymentToConfirm(booking, event.upiReference()));
    }

    /** The payment failed, or the owner did not receive a Scan & Pay payment: the booking can be paid again. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(PaymentFailedEvent event) {
        if (event.purpose() != PaymentPurpose.RENTAL_BOOKING) {
            return;
        }
        bookingRepository.findById(event.referenceId()).ifPresent(RentalBooking::paymentNotReceived);
    }

    /** Marks the booking paid once the payment is committed, and tells the owner; a repeated event changes nothing. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(PaymentSettledEvent event) {
        if (event.purpose() != PaymentPurpose.RENTAL_BOOKING) {
            return;
        }
        bookingRepository.findWithListingById(event.referenceId()).ifPresentOrElse(
                booking -> {
                    if (booking.markPaid(event.paymentId())) {
                        notifications.paid(booking);
                    }
                },
                () -> log.warn("Payment {} settled booking {}, which no longer exists", event.paymentId(),
                        event.referenceId()));
    }

    /** The vehicle's owner as the payee of Scan & Pay, when the owner has set it up. */
    private @Nullable ScanAndPayPayee payee(RentalBooking booking) {
        VehicleOwner owner = booking.getListing().getOwner();
        if (!owner.acceptsScanAndPay()) {
            return null;
        }
        String payeeName = owner.getBusinessName() != null ? owner.getBusinessName() : owner.getName();
        return new ScanAndPayPayee(owner.getUserId(), payeeName, owner.getUpiId(), pictures.upiQrUrl(owner));
    }
}
