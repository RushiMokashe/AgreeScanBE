package com.myagree.app.store;

import java.time.Instant;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.EmbeddedColumnNaming;

import com.myagree.app.common.i18n.LocalizedText;

/** The agro depot that fulfils online orders, and the current flash-deal window. Its name is a business name. */
@Entity
public class StoreDepot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private boolean open;

    @Embedded
    @EmbeddedColumnNaming("delivery_label_%s")
    private LocalizedText deliveryLabel;

    private Instant flashDealEndsAt;

    protected StoreDepot() {
    }

    public StoreDepot(String name, boolean open, LocalizedText deliveryLabel, Instant flashDealEndsAt) {
        this.name = name;
        this.open = open;
        this.deliveryLabel = deliveryLabel;
        this.flashDealEndsAt = flashDealEndsAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isOpen() {
        return open;
    }

    public LocalizedText getDeliveryLabel() {
        return deliveryLabel;
    }

    public Instant getFlashDealEndsAt() {
        return flashDealEndsAt;
    }
}
