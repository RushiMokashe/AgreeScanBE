package com.myagree.app.care;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** A licensed agro-input shop. {@code distanceKm} is measured from the demo farm. */
@Entity
public class AgroDealer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private DealerCertification certification;

    private double distanceKm;
    private String address;
    private double rating;
    private int reviewCount;
    private String phone;

    @Embedded
    private DealerStock stock;

    protected AgroDealer() {
    }

    public AgroDealer(String name, DealerCertification certification, double distanceKm, String address,
                      double rating, int reviewCount, String phone, DealerStock stock) {
        this.name = name;
        this.certification = certification;
        this.distanceKm = distanceKm;
        this.address = address;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.phone = phone;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DealerCertification getCertification() {
        return certification;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public String getAddress() {
        return address;
    }

    public double getRating() {
        return rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public String getPhone() {
        return phone;
    }

    public DealerStock getStock() {
        return stock;
    }
}
