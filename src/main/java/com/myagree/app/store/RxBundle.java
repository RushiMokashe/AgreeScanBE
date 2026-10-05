package com.myagree.app.store;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.EmbeddedColumnNaming;
import org.jspecify.annotations.Nullable;

import com.myagree.app.common.i18n.LocalizedText;

/**
 * A product bundle prescribed to one farmer for a diagnosis, e.g. "Plot A • Blight Triage Rx". The originating scan
 * is referenced by id; it belongs to the scan feature. The store banner shows the bundle's product photo, so the
 * product must have one.
 */
@Entity
@Table(indexes = @Index(name = "rx_bundle_farmer", columnList = "farmer_id, prescribed_at"))
public class RxBundle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private long farmerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    private @Nullable Long scanId;

    @Embedded
    @EmbeddedColumnNaming("label_%s")
    private LocalizedText label;

    private Instant prescribedAt;

    @Embedded
    @EmbeddedColumnNaming("genuine_label_%s")
    private LocalizedText genuineLabel;

    @Embedded
    @EmbeddedColumnNaming("subsidy_label_%s")
    private LocalizedText subsidyLabel;

    protected RxBundle() {
    }

    /**
     * @throws IllegalArgumentException when the product has no photo for the store banner
     */
    public RxBundle(long farmerId, Product product, @Nullable Long scanId, LocalizedText label, Instant prescribedAt,
                    LocalizedText genuineLabel, LocalizedText subsidyLabel) {
        if (product.getImageUrl() == null) {
            throw new IllegalArgumentException("A prescription bundle needs a product with a photo");
        }
        this.farmerId = farmerId;
        this.product = product;
        this.scanId = scanId;
        this.label = label;
        this.prescribedAt = prescribedAt;
        this.genuineLabel = genuineLabel;
        this.subsidyLabel = subsidyLabel;
    }

    public Long getId() {
        return id;
    }

    public long getFarmerId() {
        return farmerId;
    }

    public Product getProduct() {
        return product;
    }

    public @Nullable Long getScanId() {
        return scanId;
    }

    public LocalizedText getLabel() {
        return label;
    }

    public Instant getPrescribedAt() {
        return prescribedAt;
    }

    public LocalizedText getGenuineLabel() {
        return genuineLabel;
    }

    public LocalizedText getSubsidyLabel() {
        return subsidyLabel;
    }
}
