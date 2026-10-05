package com.myagree.app.care;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.jspecify.annotations.Nullable;

/** The cluster of agro-dealers serving the farmer, e.g. "Kem & Karmala Agro Hub", and its online depot offer. */
@Entity
public class AgroHub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String district;
    private int radiusKm;
    private String productsLabel;
    private String mapImageUrl;

    @Embedded
    private @Nullable OnlineOffer onlineOffer;

    protected AgroHub() {
    }

    public AgroHub(String name, String district, int radiusKm, String productsLabel, String mapImageUrl,
                   @Nullable OnlineOffer onlineOffer) {
        this.name = name;
        this.district = district;
        this.radiusKm = radiusKm;
        this.productsLabel = productsLabel;
        this.mapImageUrl = mapImageUrl;
        this.onlineOffer = onlineOffer;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDistrict() {
        return district;
    }

    public int getRadiusKm() {
        return radiusKm;
    }

    public String getProductsLabel() {
        return productsLabel;
    }

    public String getMapImageUrl() {
        return mapImageUrl;
    }

    public @Nullable OnlineOffer getOnlineOffer() {
        return onlineOffer;
    }
}
