package com.myagree.app.store;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.myagree.app.account.AccountService;
import com.myagree.app.common.ConflictException;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.Messages;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.spi.NotificationType;
import com.myagree.app.common.spi.Notifier;
import com.myagree.app.common.spi.Payable;
import com.myagree.app.common.spi.PayableResolver;
import com.myagree.app.common.spi.PaymentAwaitingConfirmationEvent;
import com.myagree.app.common.spi.PaymentFailedEvent;
import com.myagree.app.common.spi.PaymentPurpose;
import com.myagree.app.common.spi.PaymentSettledEvent;
import com.myagree.app.common.spi.ScanAndPayPayee;

/**
 * Online payment of store orders (docs/architecture/phase-2.md, D5): describes an order placed for online payment to
 * the payment feature, naming its shop as the payee of Scan & Pay, and follows the payment: paid, waiting for the
 * shopkeeper to confirm a Scan & Pay payment, or waiting for a payment again after the shopkeeper did not receive one.
 */
@Component
class StorePayments implements PayableResolver {

    private static final Logger log = LoggerFactory.getLogger(StorePayments.class);

    private static final String DESCRIPTION = "store.payment.description";
    /** The shop portal screen where shopkeepers confirm Scan & Pay payments. */
    private static final String SHOP_ORDERS_ROUTE = "/shop/orders";
    private static final UserMessage ALREADY_PAID = UserMessage.of("store.order.already-paid");
    private static final UserMessage NOT_PAYABLE = UserMessage.of("store.order.not-payable");

    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;
    private final StorePictures pictures;
    private final AccountService accountService;
    private final ObjectProvider<Notifier> notifiers;
    private final Messages messages;
    private final Clock clock;

    StorePayments(OrderRepository orderRepository, ShopRepository shopRepository, StorePictures pictures,
                  AccountService accountService, ObjectProvider<Notifier> notifiers, Messages messages, Clock clock) {
        this.orderRepository = orderRepository;
        this.shopRepository = shopRepository;
        this.pictures = pictures;
        this.accountService = accountService;
        this.notifiers = notifiers;
        this.messages = messages;
        this.clock = clock;
    }

    @Override
    public PaymentPurpose purpose() {
        return PaymentPurpose.STORE_ORDER;
    }

    /**
     * An online order is payable while it waits for its payment, for the total fixed at checkout. An order whose Scan &
     * Pay payment waits for the shopkeeper is resolved too, so the payment feature can say so.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Payable> resolvePayable(long referenceId, long farmerId, Language language) {
        return orderRepository.findByIdAndFarmerId(referenceId, farmerId).map(order -> {
            if (order.getStatus() == OrderStatus.PAID) {
                throw new ConflictException(ALREADY_PAID);
            }
            if (order.getStatus() != OrderStatus.AWAITING_PAYMENT && order.getStatus() != OrderStatus.VERIFYING_PAYMENT) {
                throw new ConflictException(NOT_PAYABLE);
            }
            return new Payable(PaymentPurpose.STORE_ORDER, order.getId(), farmerId, order.getTotal(),
                    description(order.getId(), language), order.getCustomerName(), order.getCustomerPhone(),
                    payee(order));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> describe(long referenceId, Language language) {
        return orderRepository.existsById(referenceId) ? Optional.of(description(referenceId, language)) : Optional.empty();
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

    /** The farmer paid the shop by Scan & Pay: the order waits for the shopkeeper, who is told to check their bank. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(PaymentAwaitingConfirmationEvent event) {
        if (event.purpose() != PaymentPurpose.STORE_ORDER) {
            return;
        }
        orderRepository.findById(event.referenceId())
                .filter(Order::awaitPaymentConfirmation)
                .flatMap(order -> shopRepository.findById(order.getShopId()))
                .ifPresent(shop -> notifyShopkeeper(shop, event));
    }

    /** The payment failed, or the shopkeeper did not receive a Scan & Pay payment: the order waits for one again. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(PaymentFailedEvent event) {
        if (event.purpose() != PaymentPurpose.STORE_ORDER) {
            return;
        }
        orderRepository.findById(event.referenceId()).ifPresent(Order::paymentNotReceived);
    }

    private void notifyShopkeeper(Shop shop, PaymentAwaitingConfirmationEvent event) {
        Language language = accountService.get(shop.getOwnerUserId()).preferredLanguage();
        notifiers.ifAvailable(notifier -> notifier.notify(shop.getOwnerUserId(), NotificationType.PAYMENT_TO_CONFIRM,
                Map.of(Notifier.FARMER_NAME, event.payerName(),
                        Notifier.AMOUNT, String.valueOf(event.amountRupees()),
                        Notifier.DESCRIPTION, description(event.referenceId(), language),
                        Notifier.UPI_REFERENCE, event.upiReference()),
                SHOP_ORDERS_ROUTE));
    }

    /** The order's shop as the payee of Scan & Pay, when it has set it up. */
    private @Nullable ScanAndPayPayee payee(Order order) {
        return shopRepository.findById(order.getShopId())
                .filter(Shop::acceptsScanAndPay)
                .map(shop -> new ScanAndPayPayee(shop.getOwnerUserId(), shop.getName(), shop.getUpiId(),
                        pictures.upiQrUrl(shop)))
                .orElse(null);
    }

    private String description(long orderId, Language language) {
        return messages.get(DESCRIPTION, language, String.valueOf(orderId));
    }
}
