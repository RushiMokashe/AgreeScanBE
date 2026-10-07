package com.myagree.app.payment;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.spi.Payable;
import com.myagree.app.common.spi.PaymentPurpose;

/**
 * One online payment of an order or a booking (docs/architecture/phase-2.md, D5). The amount, description and payer
 * come from the slice that owns the order or booking, never from the client. A payment is opened at its provider,
 * which gives it a reference (Stripe PaymentIntent, Razorpay order), and is settled by the client's confirmation or
 * the provider's webhook, whichever comes first.
 */
@Entity
@Table(name = "payment",
        indexes = {
                @Index(name = "payment_by_reference", columnList = "purpose, reference_id, farmer_id"),
                @Index(name = "payment_by_status", columnList = "status, settled_at")},
        uniqueConstraints = @UniqueConstraint(name = "payment_provider_reference",
                columnNames = {"provider", "provider_reference"}))
public class Payment {

    public static final String CURRENCY = "INR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farmer_id", nullable = false, updatable = false)
    private long farmerId;

    /** The payer's account, which payment notifications go to. */
    @Column(nullable = false, updatable = false)
    private long payerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private PaymentPurpose purpose;

    @Column(name = "reference_id", nullable = false, updatable = false)
    private long referenceId;

    @Column(nullable = false, updatable = false)
    private long amountRupees;

    /**
     * What the payment is for, in the payer's language when they started it, e.g. "Agro Store order #12": the text sent
     * to the provider. Responses describe the payment in the reader's language instead (PayableResolver#describe).
     */
    @Column(nullable = false, updatable = false)
    private String description;

    @Column(nullable = false, updatable = false)
    private String payerName;

    @Column(nullable = false, updatable = false, length = 10)
    private String payerPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private PaymentProviderKind provider;

    /** The provider's id for it: a Stripe PaymentIntent or a Razorpay order; set once it is opened. */
    @Column(name = "provider_reference")
    private @Nullable String providerReference;

    /** What Stripe's Payment Element needs to show the payment; only for Stripe. */
    private @Nullable String clientSecret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatusCode status;

    private @Nullable String failureReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(name = "settled_at")
    private @Nullable Instant settledAt;

    @Version
    private long version;

    protected Payment() {
    }

    Payment(Payable payable, long payerUserId, PaymentProviderKind provider, Instant at) {
        this.farmerId = payable.farmerId();
        this.payerUserId = payerUserId;
        this.purpose = payable.purpose();
        this.referenceId = payable.referenceId();
        this.amountRupees = payable.amountRupees();
        this.description = payable.description();
        this.payerName = payable.payerName();
        this.payerPhone = payable.payerPhone();
        this.provider = provider;
        this.status = PaymentStatusCode.REQUIRES_PAYMENT;
        this.createdAt = at;
        this.updatedAt = at;
    }

    /** Records how the provider knows the payment once it is opened there. */
    void opened(ProviderSession session) {
        this.providerReference = session.reference();
        this.clientSecret = session.clientSecret();
    }

    /**
     * Records that the money was received; a failed or replaced payment can still succeed, since the provider may
     * collect it after all.
     *
     * @return {@code false}, changing nothing, when it had already succeeded
     */
    boolean succeed(Instant at) {
        if (status == PaymentStatusCode.SUCCEEDED) {
            return false;
        }
        this.status = PaymentStatusCode.SUCCEEDED;
        this.failureReason = null;
        this.settledAt = at;
        this.updatedAt = at;
        return true;
    }

    /**
     * Records that the payment failed.
     *
     * @return {@code false}, changing nothing, when it had already succeeded or failed
     */
    boolean fail(String reason, Instant at) {
        if (status == PaymentStatusCode.SUCCEEDED || status == PaymentStatusCode.FAILED) {
            return false;
        }
        this.status = PaymentStatusCode.FAILED;
        this.failureReason = reason;
        this.updatedAt = at;
        return true;
    }

    /**
     * The provider is working on it; only an open payment moves on to processing.
     *
     * @return whether it moved on; {@code false} when it was already processing or finished
     */
    boolean processing(Instant at) {
        if (status != PaymentStatusCode.REQUIRES_PAYMENT) {
            return false;
        }
        this.status = PaymentStatusCode.PROCESSING;
        this.updatedAt = at;
        return true;
    }

    /**
     * A newer payment for the same order or booking takes this one's place.
     *
     * @return whether it was cancelled; {@code false} when it had already finished
     */
    boolean cancel(Instant at) {
        if (!isUnfinished()) {
            return false;
        }
        this.status = PaymentStatusCode.CANCELLED;
        this.updatedAt = at;
        return true;
    }

    boolean isUnfinished() {
        return PaymentStatusCode.UNFINISHED.contains(status);
    }

    public Long getId() {
        return id;
    }

    public long getFarmerId() {
        return farmerId;
    }

    public long getPayerUserId() {
        return payerUserId;
    }

    public PaymentPurpose getPurpose() {
        return purpose;
    }

    public long getReferenceId() {
        return referenceId;
    }

    public long getAmountRupees() {
        return amountRupees;
    }

    public String getDescription() {
        return description;
    }

    public String getPayerName() {
        return payerName;
    }

    public String getPayerPhone() {
        return payerPhone;
    }

    public PaymentProviderKind getProvider() {
        return provider;
    }

    public @Nullable String getProviderReference() {
        return providerReference;
    }

    public @Nullable String getClientSecret() {
        return clientSecret;
    }

    public PaymentStatusCode getStatus() {
        return status;
    }

    public @Nullable String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public @Nullable Instant getSettledAt() {
        return settledAt;
    }
}
