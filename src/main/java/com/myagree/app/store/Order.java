package com.myagree.app.store;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.jspecify.annotations.Nullable;

/**
 * A farmer's order. Lines copy names and prices at checkout, and the order keeps the farmer's name and mobile number
 * as the depot's delivery contact, so later catalogue or profile changes do not rewrite history.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "orders_farmer", columnList = "farmer_id, placed_at"),
        @Index(name = "orders_shop", columnList = "shop_id, placed_at")})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private long farmerId;

    /** The shop selling it; a cart holds one shop's products. */
    @Column(name = "shop_id", nullable = false)
    private long shopId;

    /** The shop's name when the order was placed, as the farmer saw it. */
    @Column(nullable = false)
    private String shopName;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant placedAt;

    private int itemCount;
    private long total;

    /** The online payment that paid the order; {@code null} until it is paid online. */
    private @Nullable Long paymentId;
    private @Nullable Instant paidAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    @Version
    private long version;

    protected Order() {
    }

    /**
     * Places an order for everything in the cart: an online order waits for its payment, a cash-on-delivery order is
     * placed right away. The caller empties the cart afterwards.
     */
    public static Order placeFrom(Cart cart, String customerName, String customerPhone, PaymentMethod paymentMethod,
                                  Instant placedAt) {
        Shop shop = cart.shop().orElseThrow(() -> new IllegalArgumentException("An empty cart cannot be ordered"));
        Order order = new Order();
        order.farmerId = cart.getFarmerId();
        order.shopId = shop.getId();
        order.shopName = shop.getName();
        order.customerName = customerName;
        order.customerPhone = customerPhone;
        order.paymentMethod = paymentMethod;
        order.status = switch (paymentMethod) {
            case ONLINE -> OrderStatus.AWAITING_PAYMENT;
            case CASH_ON_DELIVERY -> OrderStatus.PLACED;
        };
        order.placedAt = placedAt;
        order.itemCount = cart.itemCount();
        order.total = cart.total();
        cart.getItems().forEach(item -> order.lines.add(new OrderLine(order, item)));
        return order;
    }

    /**
     * The farmer paid the shop by Scan & Pay: the order waits for the shopkeeper to confirm the money arrived.
     *
     * @return {@code false}, changing nothing, when the order was not waiting for a payment
     */
    boolean awaitPaymentConfirmation() {
        if (status != OrderStatus.AWAITING_PAYMENT) {
            return false;
        }
        this.status = OrderStatus.VERIFYING_PAYMENT;
        return true;
    }

    /**
     * The shopkeeper did not receive the Scan & Pay payment: the order waits for a payment again.
     *
     * @return {@code false}, changing nothing, when no payment was being verified
     */
    boolean paymentNotReceived() {
        if (status != OrderStatus.VERIFYING_PAYMENT) {
            return false;
        }
        this.status = OrderStatus.AWAITING_PAYMENT;
        return true;
    }

    /**
     * Records the online payment that paid the order: a card or UPI payment, or a Scan & Pay payment the shopkeeper
     * confirmed.
     *
     * @return {@code false}, changing nothing, when the order was not waiting for a payment
     */
    boolean markPaid(long paymentId, Instant paidAt) {
        if (status != OrderStatus.AWAITING_PAYMENT && status != OrderStatus.VERIFYING_PAYMENT) {
            return false;
        }
        this.status = OrderStatus.PAID;
        this.paymentId = paymentId;
        this.paidAt = paidAt;
        return true;
    }

    public Long getId() {
        return id;
    }

    public long getFarmerId() {
        return farmerId;
    }

    public long getShopId() {
        return shopId;
    }

    public String getShopName() {
        return shopName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public int getItemCount() {
        return itemCount;
    }

    public long getTotal() {
        return total;
    }

    public @Nullable Long getPaymentId() {
        return paymentId;
    }

    public List<OrderLine> getLines() {
        return List.copyOf(lines);
    }
}
