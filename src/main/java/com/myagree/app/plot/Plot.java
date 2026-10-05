package com.myagree.app.plot;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

import org.jspecify.annotations.Nullable;

/**
 * A field plot and the crop growing on it. The latest scan and the running treatment plan are referenced
 * by id: they are separate aggregates owned by the scan and treatment features.
 */
@Entity
public class Plot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String fieldName;

    @Enumerated(EnumType.STRING)
    private Crop crop;

    private String variety;
    private double areaAcres;
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private PlotHealth health;

    @ElementCollection
    @CollectionTable(name = "plot_metric", joinColumns = @JoinColumn(name = "plot_id"))
    @OrderColumn(name = "position")
    private List<PlotMetric> metrics = new ArrayList<>();

    private @Nullable String pendingAction;
    private @Nullable Long activeTreatmentPlanId;
    private @Nullable Long latestScanId;

    protected Plot() {
    }

    public Plot(String code, String fieldName, Crop crop, String variety, double areaAcres, String imageUrl,
                PlotHealth health, List<PlotMetric> metrics, @Nullable String pendingAction) {
        this.code = code;
        this.fieldName = fieldName;
        this.crop = crop;
        this.variety = variety;
        this.areaAcres = areaAcres;
        this.imageUrl = imageUrl;
        this.health = health;
        this.metrics = new ArrayList<>(metrics);
        this.pendingAction = pendingAction;
    }

    /** How scans and plans refer to this plot, e.g. "North Field • Plot A". */
    public String label() {
        return fieldName + " • " + code;
    }

    public void recordLatestScan(long scanId) {
        this.latestScanId = scanId;
    }

    public void assignTreatmentPlan(long treatmentPlanId) {
        this.activeTreatmentPlanId = treatmentPlanId;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Crop getCrop() {
        return crop;
    }

    public String getVariety() {
        return variety;
    }

    public double getAreaAcres() {
        return areaAcres;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public PlotHealth getHealth() {
        return health;
    }

    public List<PlotMetric> getMetrics() {
        return List.copyOf(metrics);
    }

    public @Nullable String getPendingAction() {
        return pendingAction;
    }

    public @Nullable Long getActiveTreatmentPlanId() {
        return activeTreatmentPlanId;
    }

    public @Nullable Long getLatestScanId() {
        return latestScanId;
    }
}
