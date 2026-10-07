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
import com.myagree.app.common.spi.PaymentFailedEvent;
import com.myagree.app.common.spi.PaymentPurpose;
import com.myagree.app.common.spi.PaymentSettledEvent;
import com.myagree.app.payment.dto.AdminPaymentResponse;
import com.myagree.app.payment.dto.ConfirmPaymentRequest;
import com.myagree.app.payment.dto.CreatePaymentRequest;
import com.myagree.app.payment.dto.PaymentCheckoutResponse;
import com.myagree.app.payment.dto.PaymentStatusResponse;

/**
 * Online payments of orders and bookings (docs/architecture/phase-2.md, D5). The order's or booking's own slice says
 * what is due through its {@link PayableResolver}; the payment is opened at the active provider and settled by the
 * farmer's confirmation or the provider's webhook, whichever comes first, exactly once: the settlement is published
 * as a {@link PaymentSettledEvent} or {@link PaymentFailedEvent} and the payer is notified. Every change is also
 * published as a {@link PaymentActivity}, which {@link PaymentActivityLog} keeps in MongoDB.
 */
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private static final String NOT_FOUND = "payment.not-found";
    private static final UserMessage PAYABLE_NOT_FOUND = UserMessage.of("payment.payable-not-found");

    private final PaymentRepository repository;
    private final List<PayableResolver> resolvers;
    private final PaymentProviders providers;
    private final PaymentNotifications notifications;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    PaymentService(PaymentRepository repository, List<PayableResolver> resolvers, PaymentProviders providers,
                   PaymentNotifications notifications, ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.resolvers = resolvers;
        this.providers = providers;
        this.notifications = notifications;
        this.events = events;
        this.clock = clock;
    }

    /**
     * The checkout of an order or booking: its unfinished payment when it has one for the amount still due, else a new
     * payment opened at the active provider (replacing an unfinished one for another amount).
     *
     * @throws NotFoundException when the farmer has no such order or booking
     * @throws ConflictException when it cannot be paid now: already paid, or not accepted yet
     */
    @Transactional
    public PaymentCheckoutResponse create(CurrentUser user, CreatePaymentRequest request, Language language) {
        long farmerId = user.requireFarmerId();
        Payable payable = resolver(request.purpose())
                .resolvePayable(request.referenceId(), farmerId, language)
                .orElseThrow(() -> new NotFoundException(PAYABLE_NOT_FOUND));
        Instant now = clock.instant();
        Optional<Payment> unfinished = repository.findFirstByFarmerIdAndPurposeAndReferenceIdAndStatusInOrderByIdDesc(
                farmerId, request.purpose(), request.referenceId(), PaymentStatusCode.UNFINISHED);
        if (unfinished.isPresent() && reusable(unfinished.get(), payable)) {
            Payment payment = unfinished.get();
            return PaymentMapper.toCheckout(payment, providers.of(payment), payable.description());
        }
        unfinished.ifPresent(stale -> {
            if (stale.cancel(now)) {
                activity(stale, PaymentActivity.Type.CANCELLED, PaymentActivity.Source.CHECKOUT, now);
            }
        });
        PaymentProvider provider = providers.active();
        Payment payment = repository.save(new Payment(payable, user.userId(), provider.kind(), now));
        payment.opened(provider.open(payment));
        activity(payment, PaymentActivity.Type.OPENED, PaymentActivity.Source.CHECKOUT, now);
        return PaymentMapper.toCheckout(payment, provider, payable.description());
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
     * it is, so confirming twice is harmless.
     *
     * @throws NotFoundException when the farmer has no such payment
     */
    @Transactional
    public PaymentStatusResponse confirm(long farmerId, long paymentId, ConfirmPaymentRequest request,
                                         Language language) {
        Payment payment = find(farmerId, paymentId);
        if (payment.isUnfinished()) {
            record(payment, providers.of(payment).confirm(payment, request), PaymentActivity.Source.CONFIRMATION);
        }
        return PaymentMapper.toStatus(payment, describe(payment, language));
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

    /** Applies what the provider says, as reported by {@code source}; only a real change is published. */
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

    private void activity(Payment payment, PaymentActivity.Type type, PaymentActivity.Source source, Instant at) {
        events.publishEvent(PaymentActivity.of(payment, type, source, at));
    }

    /** An unfinished payment is paid again while it is still for the amount due and its provider still takes payments. */
    private boolean reusable(Payment payment, Payable payable) {
        return payment.getAmountRupees() == payable.amountRupees() && providers.isAvailable(payment.getProvider());
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
        return repository.findByIdAndFarmerId(paymentId, farmerId)
                .orElseThrow(() -> new NotFoundException(UserMessage.of(NOT_FOUND, String.valueOf(paymentId))));
    }
}
