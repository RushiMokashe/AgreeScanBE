package com.myagree.app.rental;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.UpiId;
import com.myagree.app.common.media.StoredPhoto;

/**
 * The vehicle-owner profile of an account with the VEHICLE_OWNER role (docs/architecture/phase-2.md, D1): the name
 * farmers see as the operator of the owner's vehicles, the business it trades as, and the hub it serves.
 */
@Entity
@Table(name = "vehicle_owner")
public class VehicleOwner {

    public static final int NAME_MAX_LENGTH = 80;

    /** Farmers dial owners from the listing card; accounts keep the 10-digit national number. */
    private static final String INDIA_DIALING_PREFIX = "+91";
    private static final int MAX_INITIALS = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The account this profile belongs to. */
    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private long userId;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(length = NAME_MAX_LENGTH)
    private @Nullable String businessName;

    /** The account's 10-digit mobile number. */
    @Column(nullable = false, length = 10)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hub_id")
    private RentalHub hub;

    /** Where Scan & Pay sends a farmer's payment for a booking, e.g. "rameshwar.patil@okaxis"; {@code null} for none. */
    @Column(length = UpiId.MAX_LENGTH)
    private @Nullable String upiId;

    /** The UPI QR the owner uploaded from their payments app, served through a signed media URL. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fileName", column = @Column(name = "upi_qr_file_name", length = 36)),
            @AttributeOverride(name = "contentType", column = @Column(name = "upi_qr_content_type", length = 100))})
    private @Nullable StoredPhoto upiQr;

    protected VehicleOwner() {
    }

    public VehicleOwner(long userId, String name, @Nullable String businessName, String phone, RentalHub hub) {
        this.userId = userId;
        this.phone = phone;
        update(name, businessName, hub);
    }

    /** Changes what farmers see and the hub the owner serves; the phone stays the account's. */
    public void update(String name, @Nullable String businessName, RentalHub hub) {
        this.name = name.strip();
        this.businessName = businessName == null || businessName.isBlank() ? null : businessName.strip();
        this.hub = hub;
    }

    /** Where Scan & Pay sends farmers' payments; a {@code null} or blank UPI ID removes it. */
    void changeUpiId(@Nullable String upiId) {
        this.upiId = upiId == null || upiId.isBlank() ? null : upiId.strip();
    }

    void replaceUpiQr(StoredPhoto upiQr) {
        this.upiQr = upiQr;
    }

    void removeUpiQr() {
        this.upiQr = null;
    }

    /** Whether farmers can pay the owner directly: the owner has a UPI ID or a QR. */
    public boolean acceptsScanAndPay() {
        return upiId != null || upiQr != null;
    }

    public @Nullable String getUpiId() {
        return upiId;
    }

    public @Nullable StoredPhoto getUpiQr() {
        return upiQr;
    }

    /** "RP" for "Rameshwar Patil", "AL" for "AgriScan Logistics", "V" for "Vinod". */
    public String initials() {
        return Arrays.stream(name.split("\\s+"))
                .filter(word -> !word.isEmpty())
                .limit(MAX_INITIALS)
                .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT))
                .collect(Collectors.joining());
    }

    /** "Rameshwar" for "Rameshwar Patil". */
    public String firstName() {
        return name.split("\\s+", 2)[0];
    }

    /** The number for tel: links, e.g. "+919800012345". */
    public String dialablePhone() {
        return INDIA_DIALING_PREFIX + phone;
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public @Nullable String getBusinessName() {
        return businessName;
    }

    public String getPhone() {
        return phone;
    }

    public RentalHub getHub() {
        return hub;
    }
}
