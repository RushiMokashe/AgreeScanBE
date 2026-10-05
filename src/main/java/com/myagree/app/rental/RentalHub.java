package com.myagree.app.rental;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** The rental hub serving the farmer's village, e.g. "Solapur APMC Hub" covering a 12 km radius. */
@Entity
public class RentalHub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int radiusKm;
    private String routes;
    private int onlineCount;

    protected RentalHub() {
    }

    public RentalHub(String name, int radiusKm, String routes, int onlineCount) {
        this.name = name;
        this.radiusKm = radiusKm;
        this.routes = routes;
        this.onlineCount = onlineCount;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getRadiusKm() {
        return radiusKm;
    }

    public String getRoutes() {
        return routes;
    }

    public int getOnlineCount() {
        return onlineCount;
    }
}
