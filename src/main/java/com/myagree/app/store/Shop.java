package com.myagree.app.store;

import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.UpiId;
import com.myagree.app.common.media.StoredPhoto;

/**
 * A shop that sells in the Agro Store, kept by one shopkeeper account. Every product belongs to a shop, and a cart
 * (so an order) holds one shop's products, since a farmer may pay the shop directly by Scan & Pay: with its UPI ID, its
 * own UPI QR, or both.
 */
@Entity
@Table(name = "shop")
public class Shop {

    public static final int NAME_MAX_LENGTH = 80;
    public static final int PLACE_MAX_LENGTH = 80;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The shopkeeper's account. */
    @Column(name = "owner_user_id", nullable = false, unique = true, updatable = false)
    private long ownerUserId;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    /** Village or town and district, e.g. "Karmala, Solapur". */
    @Column(nullable = false, length = PLACE_MAX_LENGTH)
    private String place;

    /** The shopkeeper's 10-digit mobile number. */
    @Column(nullable = false, length = 10)
    private String phone;

    /** Where Scan & Pay sends the money, e.g. "solapur.agro@okaxis"; {@code null} until the shopkeeper sets it. */
    @Column(length = UpiId.MAX_LENGTH)
    private @Nullable String upiId;

    /** The UPI QR the shopkeeper uploaded from their payments app, served through a signed media URL. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fileName", column = @Column(name = "upi_qr_file_name", length = 36)),
            @AttributeOverride(name = "contentType", column = @Column(name = "upi_qr_content_type", length = 100))})
    private @Nullable StoredPhoto upiQr;

    @Version
    private long version;

    protected Shop() {
    }

    public Shop(long ownerUserId, String name, String place, String phone) {
        this.ownerUserId = ownerUserId;
        this.name = Objects.requireNonNull(name);
        this.place = Objects.requireNonNull(place);
        this.phone = Objects.requireNonNull(phone);
    }

    /** The shop's details as the shopkeeper edits them; a {@code null} UPI ID stops Scan & Pay by UPI ID. */
    void updateProfile(String name, String place, @Nullable String upiId) {
        this.name = name;
        this.place = place;
        this.upiId = upiId;
    }

    void replaceUpiQr(StoredPhoto upiQr) {
        this.upiQr = upiQr;
    }

    void removeUpiQr() {
        this.upiQr = null;
    }

    /** Whether farmers can pay this shop directly: it has a UPI ID or a QR. */
    public boolean acceptsScanAndPay() {
        return upiId != null || upiQr != null;
    }

    public Long getId() {
        return id;
    }

    public long getOwnerUserId() {
        return ownerUserId;
    }

    public String getName() {
        return name;
    }

    public String getPlace() {
        return place;
    }

    public String getPhone() {
        return phone;
    }

    public @Nullable String getUpiId() {
        return upiId;
    }

    public @Nullable StoredPhoto getUpiQr() {
        return upiQr;
    }
}
