package com.myagree.app.store;

import java.time.Clock;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.myagree.app.common.ConflictException;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.Messages;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.spi.Payable;
import com.myagree.app.common.spi.PayableResolver;
import com.myagree.app.common.spi.PaymentPurpose;
import com.myagree.app.common.spi.PaymentSettledEvent;

/**
 * Online payment of store orders (docs/architecture/phase-2.md, D5): describes an order placed for online payment to
 * the payment feature, and marks it paid once the payment has gone through.
 */
@Component
class StorePayments implements PayableResolver {

    private static final Logger log = LoggerFactory.getLogger(StorePayments.class);

    private static final String DESCRIPTION = "store.payment.description";
    private static final UserMessage ALREADY_PAID = UserMessage.of("store.order.already-paid");
    private static final UserMessage NOT_PAYABLE = UserMessage.of("store.order.not-payable");

    private final OrderRepository orderRepository;
    private final Messages messages;
    private final Clock clock;

    StorePayments(OrderRepository orderRepository, Messages messages, Clock clock) {
        this.orderRepository = orderRepository;
        this.messages = messages;
        this.clock = clock;
    }

    @Override
    public PaymentPurpose purpose() {
        return PaymentPurpose.STORE_ORDER;
    }

    /** An online order is payable while it waits for its payment, for the total fixed at checkout. */
    @Override
    @Transactional(readOnly = true)
    public Optional<Payable> resolvePayable(long referenceId, long farmerId, Language language) {
        return orderRepository.findByIdAndFarmerId(referenceId, farmerId).map(order -> {
            if (order.getStatus() == OrderStatus.PAID) {
                throw new ConflictException(ALREADY_PAID);
            }
            if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
                throw new ConflictException(NOT_PAYABLE);
            }
            return new Payable(PaymentPurpose.STORE_ORDER, order.getId(), farmerId, order.getTotal(),
                    description(order.getId(), language), order.getCustomerName(), order.getCustomerPhone());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> describe(long referenceId, Language language) {
        return orderRepository.existsById(referenceId) ? Optional.of(description(referenceId, language)) : Optional.empty();
    }

    private String description(long orderId, Language language) {
        return messages.get(DESCRIPTION, language, String.valueOf(orderId));
    }

    /** Marks the order paid once the payment is committed; a repeated event changes nothing. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(PaymentSettledEvent event) {
        if (event.purpose() != PaymentPurpose.STORE_ORDER) {
            return;
        }
        orderRepository.findById(event.referenceId()).ifPresentOrElse(
                order -> order.markPaid(event.paymentId(), clock.instant()),
                () -> log.warn("Payment {} settled order {}, which no longer exists", event.paymentId(), event.referenceId()));
    }
}
