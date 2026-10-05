package com.myagree.app.store;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.EmbeddedColumnNaming;

import com.myagree.app.common.i18n.LocalizedText;

/** A product line of a placed order, frozen at checkout time in every language. */
@Entity
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    private long productId;

    @Embedded
    @EmbeddedColumnNaming("name_%s")
    private LocalizedText name;

    @Embedded
    @EmbeddedColumnNaming("short_name_%s")
    private LocalizedText shortName;

    private int unitPrice;
    private int quantity;

    protected OrderLine() {
    }

    OrderLine(Order order, CartItem item) {
        this.order = order;
        this.productId = item.getProduct().getId();
        this.name = item.getProduct().getName();
        this.shortName = item.getProduct().getShortName();
        this.unitPrice = item.unitPrice();
        this.quantity = item.getQuantity();
    }

    public long lineTotal() {
        return (long) unitPrice * quantity;
    }

    public Long getId() {
        return id;
    }

    public long getProductId() {
        return productId;
    }

    public LocalizedText getName() {
        return name;
    }

    public LocalizedText getShortName() {
        return shortName;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }
}
