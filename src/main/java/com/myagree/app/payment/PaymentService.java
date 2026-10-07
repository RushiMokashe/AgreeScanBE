package com.myagree.app.payment;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.ConflictException;
import com.myagree.app.common.NotFoundException;
import com.myagree.app.common.PageResponse;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.common.spi.Payable;
import com.myagree.app.common.spi.PayableResolver;
import com.myagree.app.common.spi.PaymentAwaitingConfirmationEvent;
import com.myagree.app.common.spi.PaymentFailedEvent;
import com.myagree.app.common.spi.PaymentPurpose;
import com.myagree.app.common.spi.PaymentSettledEvent;
import com.myagree.app.common.spi.ScanAndPayPayee;
import com.myagree.app.payment.dto.AdminPaymentResponse;
import com.myagree.app.payment.dto.ConfirmPaymentRequest;
import com.myagree.app.payment.dto.CreatePaymentRequest;
import com.myagree.app.payment.dto.PaymentCheckoutResponse;
import com.myagree.app.payment.dto.PaymentCheckoutResponse.ScanAndPayOption;
import com.myagree.app.payment.dto.PaymentStatusResponse;
import com.myagree.app.payment.dto.ScanAndPayRequest;
import com.myagree.app.payment.dto.SellerPaymentResponse;

/**
 * Online payments of orders and bookings (docs/architecture/phase-2.md, D5). The order's or booking's own slice says
 * what is due through its {@link PayableResolver}. A card or UPI payment is opened at the active provider and settled
 * by the farmer's confirmation or the provider's webhook, whichever comes first. A Scan & Pay payment goes straight to
 * the seller and is settled by the seller's word. Either way it is settled exactly once: published as a
 * {@link PaymentSettledEvent} or {@link PaymentFailedEvent}, with the payer notified. Every change is also published
 * as a {@link PaymentActivity}, which {@link PaymentActivityLog} keeps in MongoDB.
 */
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private static final String NOT_FOUND = "payment.not-found";
    private static final String SCAN_AND_PAY_PENDING = "payment.scan-and-pay-pending";
    private static final UserMessage PAYABLE_NOT_FOUND = UserMessage.of("payment.payable-not-found");
    private static final UserMessage SCAN_AND_PAY_UNAVAILABLE = UserMessage.of("payment.scan-and-pay-unavailable");
    private static final UserMessage UPI_REFERENCE_USED = UserMessage.of("payment.upi-reference-used");
    private static final UserMessage ALREADY_DECIDED = UserMessage.of("payment.already-decided");
    /** The latest Scan & Pay payments a seller sees. */
    private static final int SELLER_PAYMENTS_LIMIT = 50;

    private final PaymentRepository repository;
    private final List<PayableResolver> resolvers;
    private final PaymentProviders providers;
    private final PaymentNotifications notifications;
    private final UpiQrCodes upiQrCodes;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    PaymentService(PaymentRepository repository, List<PayableResolver> resolvers, PaymentProviders providers,
                   PaymentNotifications notifications, UpiQrCodes upiQrCodes, ApplicationEventPublisher events,
                   Clock clock) {
        this.repository = repository;
        this.resolvers = resolvers;
        this.providers = providers;
        this.notifications = notifications;
        this.upiQrCodes = upiQrCodes;
        this.events = events;
        this.clock = clock;
    }

    /**
     * The checkout of an order or booking: its unfinished payment when it has one for the amount still due, else a new
     * payment opened at the active provider (replacing an unfinished one for another amount). It also offers Scan &
     * Pay when the seller has set it up.
     *
     * @throws NotFoundException when the farmer has no such order or booking
     * @throws ConflictException when it cannot be paid now: already paid, not accepted yet, or a Scan & Pay payment
     *                           of it waits for the seller
     */
    @Transactional
    public PaymentCheckoutResponse create(CurrentUser user, CreatePaymentRequest request, Language language) {
        long farmerId = user.requireFarmerId();
        Payable payable = resolvePayable(request.purpose(), request.referenceId(), farmerId, language);
        Instant now = clock.instant();
        Optional<Payment> unfinished = unfinishedPayment(farmerId, request.purpose(), request.referenceId());
        if (unfinished.isPresent() && reusable(unfinished.get(), payable)) {
            Payment payment = unfinished.get();
            return PaymentMapper.toCheckout(payment, providers.of(payment), payable.description(), scanAndPay(payable));
        }
        unfinished.ifPresent(stale -> cancel(stale, now));
        PaymentProvider provider = providers.active();
        Payment payment = repository.save(new Payment(payable, user.userId(), provider.kind(), now));
        payment.opened(provider.open(payment));
        activity(payment, PaymentActivity.Type.OPENED, PaymentActivity.Source.CHECKOUT, now);
        return PaymentMapper.toCheckout(payment, provider, payable.description(), scanAndPay(payable));
    }

    /**
     * Records that the farmer paid the seller directly by Scan & Pay, quoting the UPI transaction reference. The
     * payment then waits for the seller to confirm the money arrived; an open card or UPI payment of the same order or
     * booking is closed.
     *
     * @throws NotFoundException when the farmer has no such order or booking
     * @throws ConflictException when it cannot be paid now, the seller offers no Scan & Pay, the reference was used
     *                           before, or an earlier Scan & Pay payment of it still waits for the seller
     */
    @Transactional
    public PaymentStatusResponse submitScanAndPay(CurrentUser user, ScanAndPayRequest request, Language language) {
        long farmerId = user.requireFarmerId();
        Payable payable = resolvePayable(request.purpose(), request.referenceId(), farmerId, language);
        ScanAndPayPayee payee = Optional.ofNullable(payable.payee())
                .orElseThrow(() -> new ConflictException(SCAN_AND_PAY_UNAVAILABLE));
        if (repository.findByProviderAndProviderReference(PaymentProviderKind.SCAN_AND_PAY, request.upiReference())
                .isPresent()) {
            throw new ConflictException(UPI_REFERENCE_USED);
        }
        Instant now = clock.instant();
        unfinishedPayment(farmerId, request.purpose(), request.referenceId()).ifPresent(open -> cancel(open, now));
        Payment payment = repository.save(Payment.scanAndPay(payable, payee, user.userId(), request.upiReference(), now));
        activity(payment, PaymentActivity.Type.OPENED, PaymentActivity.Source.CHECKOUT, now);
        events.publishEvent(new PaymentAwaitingConfirmationEvent(payment.getPurpose(), payment.getReferenceId(),
                payment.getId(), farmerId, payment.getPayerName(), payment.getAmountRupees(), request.upiReference(), now));
        return PaymentMapper.toStatus(payment, payable.description());
    }

    /**
     * One of the farmer's payments.
     *
     * @throws NotFoundException when the farmer has no such payment
     */
    public PaymentStatusResponse status(long farmerId, long paymentId, Language language) {
        Payment payment = find(farmerId, paymentId);
        return PaymentMapper.toStatus(payment, describe(payment, language));
    }

    /**
     * Settles an unfinished payment with what the provider says after the farmer paid; a settled payment stays as
     * it is, so confirming twice is harmless. A Scan & Pay payment only changes on the seller's word.
     *
     * @throws NotFoundException when the farmer has no such payment
     */
    @Transactional
    public PaymentStatusResponse confirm(long farmerId, long paymentId, ConfirmPaymentRequest request,
                                         Language language) {
        Payment payment = find(farmerId, paymentId);
        if (payment.isUnfinished() && !payment.isScanAndPay()) {
            record(payment, providers.of(payment).confirm(payment, request), PaymentActivity.Source.CONFIRMATION);
        }
        return PaymentMapper.toStatus(payment, describe(payment, language));
    }

    /** The Scan & Pay payments farmers sent to the seller, optionally only those in {@code status}, newest first. */
    public List<SellerPaymentResponse> sellerPayments(long payeeUserId, @Nullable PaymentStatusCode status,
                                                      Language language) {
        return repository.findForPayee(payeeUserId, status, PageRequest.of(0, SELLER_PAYMENTS_LIMIT)).stream()
                .map(payment -> PaymentMapper.toSellerResponse(payment, describe(payment, language)))
                .toList();
    }

    /**
     * The seller saw the money arrive: the payment succeeds and the order or booking is paid.
     *
     * @throws NotFoundException when the seller was not paid by this payment
     * @throws ConflictException when the seller already decided
     */
    @Transactional
    public SellerPaymentResponse markReceived(long payeeUserId, long paymentId, Language language) {
        Payment payment = awaitingSeller(payeeUserId, paymentId);
        record(payment, ProviderOutcome.SUCCEEDED, PaymentActivity.Source.SELLER);
        return PaymentMapper.toSellerResponse(payment, describe(payment, language));
    }

    /**
     * The money never reached the seller: the payment fails, and the farmer can pay again.
     *
     * @param reason what the seller wants the farmer to know; {@code null} for nothing
     * @throws NotFoundException when the seller was not paid by this payment
     * @throws ConflictException when the seller already decided
     */
    @Transactional
    public SellerPaymentResponse markNotReceived(long payeeUserId, long paymentId, @Nullable String reason,
                                                 Language language) {
        Payment payment = awaitingSeller(payeeUserId, paymentId);
        record(payment, ProviderOutcome.failed(Objects.requireNonNullElse(reason, "").strip()),
                PaymentActivity.Source.SELLER);
        return PaymentMapper.toSellerResponse(payment, describe(payment, language));
    }

    /** Settles the payment a verified provider webhook reports on; one the provider knows but AgriScan does not is ignored. */
    @Transactional
    void applyWebhook(PaymentProviderKind provider, WebhookOutcome outcome) {
        repository.findByProviderAndProviderReference(provider, outcome.reference()).ifPresentOrElse(
                payment -> record(payment, outcome.outcome(), PaymentActivity.Source.WEBHOOK),
                () -> log.info("Ignored a {} webhook about unknown payment {}", provider, outcome.reference()));
    }

    /** Every payment, optionally only those in {@code status}, newest first, for the admin portal. */
    public PageResponse<AdminPaymentResponse> adminPayments(@Nullable PaymentStatusCode status, int page, int size) {
        return PageResponse.of(repository.findAllByStatus(status, PageRequest.of(page, size))
                .map(PaymentMapper::toAdminResponse));
    }

    /** Applies what the provider (or, for Scan & Pay, the seller) says, as reported by {@code source}. */
    private void record(Payment payment, ProviderOutcome outcome, PaymentActivity.Source source) {
        Instant now = clock.instant();
        switch (outcome.status()) {
            case SUCCEEDED -> {
                if (payment.succeed(now)) {
                    events.publishEvent(new PaymentSettledEvent(payment.getPurpose(), payment.getReferenceId(),
                            payment.getId(), payment.getFarmerId(), payment.getAmountRupees(), now));
                    activity(payment, PaymentActivity.Type.SUCCEEDED, source, now);
                    notifications.succeeded(payment);
                }
            }
            case FAILED -> {
                String reason = Objects.requireNonNullElse(outcome.failureReason(), "");
                if (payment.fail(reason, now)) {
                    events.publishEvent(new PaymentFailedEvent(payment.getPurpose(), payment.getReferenceId(),
                            payment.getId(), payment.getFarmerId(), payment.getAmountRupees(), now, reason));
                    activity(payment, PaymentActivity.Type.FAILED, source, now);
                    notifications.failed(payment);
                }
            }
            case PROCESSING -> {
                if (payment.processing(now)) {
                    activity(payment, PaymentActivity.Type.PROCESSING, source, now);
                }
            }
            case REQUIRES_PAYMENT, CANCELLED -> {
                // Nothing happened at the provider yet.
            }
        }
    }

    /** A newer payment takes the place of an open one; a Scan & Pay payment waiting for the seller cannot be replaced. */
    private void cancel(Payment open, Instant now) {
        if (open.isScanAndPay()) {
            throw new ConflictException(UserMessage.of(SCAN_AND_PAY_PENDING, Objects.requireNonNull(open.getPayeeName())));
        }
        if (open.cancel(now)) {
            activity(open, PaymentActivity.Type.CANCELLED, PaymentActivity.Source.CHECKOUT, now);
        }
    }

    /**
     * Paying the seller directly, when they have set it up: their QR, and with their UPI ID a link and a QR that fill
     * in the amount.
     */
    private @Nullable ScanAndPayOption scanAndPay(Payable payable) {
        ScanAndPayPayee payee = payable.payee();
        if (payee == null) {
            return null;
        }
        String upiId = payee.upiId();
        String link = upiId != null
                ? upiQrCodes.link(upiId, payee.payeeName(), payable.amountRupees(), payable.description())
                : null;
        return new ScanAndPayOption(payee.payeeName(), upiId, link, payee.qrImageUrl(),
                link != null ? upiQrCodes.qrDataUrl(link) : null);
    }

    private void activity(Payment payment, PaymentActivity.Type type, PaymentActivity.Source source, Instant at) {
        events.publishEvent(PaymentActivity.of(payment, type, source, at));
    }

    /**
     * An unfinished payment is paid again while it is still for the amount due and its provider still takes payments;
     * a Scan & Pay payment waiting for the seller is never reused (it is refused where it would be replaced).
     */
    private boolean reusable(Payment payment, Payable payable) {
        return !payment.isScanAndPay()
                && payment.getAmountRupees() == payable.amountRupees()
                && providers.isAvailable(payment.getProvider());
    }

    private Optional<Payment> unfinishedPayment(long farmerId, PaymentPurpose purpose, long referenceId) {
        return repository.findFirstByFarmerIdAndPurposeAndReferenceIdAndStatusInOrderByIdDesc(
                farmerId, purpose, referenceId, PaymentStatusCode.UNFINISHED);
    }

    private Payable resolvePayable(PaymentPurpose purpose, long referenceId, long farmerId, Language language) {
        return resolver(purpose)
                .resolvePayable(referenceId, farmerId, language)
                .orElseThrow(() -> new NotFoundException(PAYABLE_NOT_FOUND));
    }

    /** What {@code payment} is for in the reader's language; the one stored when it opened if its order or booking is gone. */
    private String describe(Payment payment, Language language) {
        return findResolver(payment.getPurpose())
                .flatMap(resolver -> resolver.describe(payment.getReferenceId(), language))
                .orElse(payment.getDescription());
    }

    private PayableResolver resolver(PaymentPurpose purpose) {
        return findResolver(purpose).orElseThrow(() -> new NotFoundException(PAYABLE_NOT_FOUND));
    }

    private Optional<PayableResolver> findResolver(PaymentPurpose purpose) {
        return resolvers.stream().filter(resolver -> resolver.purpose() == purpose).findFirst();
    }

    private Payment find(long farmerId, long paymentId) {
        return repository.findByIdAndFarmerId(paymentId, farmerId).orElseThrow(() -> notFound(paymentId));
    }

    /** A Scan & Pay payment sent to the seller that still waits for their decision. */
    private Payment awaitingSeller(long payeeUserId, long paymentId) {
        Payment payment = repository.findByIdAndPayeeUserId(paymentId, payeeUserId)
                .orElseThrow(() -> notFound(paymentId));
        if (!payment.isUnfinished()) {
            throw new ConflictException(ALREADY_DECIDED);
        }
        return payment;
    }

    private static NotFoundException notFound(long paymentId) {
        return new NotFoundException(UserMessage.of(NOT_FOUND, String.valueOf(paymentId)));
    }
}
