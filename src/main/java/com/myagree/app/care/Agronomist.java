package com.myagree.app.care;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** A plant-health expert farmers can call for free. */
@Entity
public class Agronomist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String title;
    private String photoUrl;
    private String phone;
    private boolean onDuty;

    @Column(length = 500)
    private String pitch;

    protected Agronomist() {
    }

    public Agronomist(String name, String title, String photoUrl, String phone, boolean onDuty, String pitch) {
        this.name = name;
        this.title = title;
        this.photoUrl = photoUrl;
        this.phone = phone;
        this.onDuty = onDuty;
        this.pitch = pitch;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isOnDuty() {
        return onDuty;
    }

    public String getPitch() {
        return pitch;
    }
}
