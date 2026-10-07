package com.myagree.app.rental;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.EmbeddedColumnNaming;

import com.myagree.app.common.i18n.LocalizedText;

/** A rental hub: the vehicles serving the villages around a market town, e.g. "Solapur APMC Hub" within 12 km. */
@Entity
public class RentalHub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @EmbeddedColumnNaming("name_%s")
    private LocalizedText name;

    private int radiusKm;

    /** "Karmala, Kem & Kurduwadi routes" */
    @Embedded
    @EmbeddedColumnNaming("routes_%s")
    private LocalizedText routes;

    protected RentalHub() {
    }

    public RentalHub(LocalizedText name, int radiusKm, LocalizedText routes) {
        this.name = name;
        this.radiusKm = radiusKm;
        this.routes = routes;
    }

    public Long getId() {
        return id;
    }

    public LocalizedText getName() {
        return name;
    }

    public int getRadiusKm() {
        return radiusKm;
    }

    public LocalizedText getRoutes() {
        return routes;
    }
}
