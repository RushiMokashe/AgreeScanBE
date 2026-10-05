package com.myagree.app.dashboard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** A regional crop-disease warning shown on the home screen. */
@Entity
public class RiskAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    private AlertSeverity severity;

    private String region;

    protected RiskAlert() {
    }

    public RiskAlert(String title, String message, AlertSeverity severity, String region) {
        this.title = title;
        this.message = message;
        this.severity = severity;
        this.region = region;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public String getRegion() {
        return region;
    }
}
