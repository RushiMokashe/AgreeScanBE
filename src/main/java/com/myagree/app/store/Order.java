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
@Table(name = "orders", indexes = @Index(name = "orders_farmer", columnList = "farmer_id, placed_at"))
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private long farmerId;

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
        Order order = new Order();
        order.farmerId = cart.getFarmerId();
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
     * Records the online payment that paid the order.
     *
     * @return {@code false}, changing nothing, when the order was not waiting for a payment
     */
    boolean markPaid(long paymentId, Instant paidAt) {
        if (status != OrderStatus.AWAITING_PAYMENT) {
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
