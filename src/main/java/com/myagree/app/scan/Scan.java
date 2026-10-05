package com.myagree.app.scan;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

import org.jspecify.annotations.Nullable;

import com.myagree.app.plot.Crop;
import com.myagree.app.plot.PlotLink;
import com.myagree.app.scan.diagnosis.Diagnosis;
import com.myagree.app.scan.diagnosis.DiseaseProfile;
import com.myagree.app.scan.diagnosis.OrganicAlternative;
import com.myagree.app.scan.diagnosis.Prescription;
import com.myagree.app.scan.diagnosis.ScanMode;
import com.myagree.app.scan.diagnosis.Severity;

/**
 * A diagnosed crop photo. The diagnosis is copied in at creation, so the record keeps what the farmer
 * was told even if the knowledge base or model changes later.
 */
@Entity
public class Scan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Crop crop;

    @Enumerated(EnumType.STRING)
    private ScanMode mode;

    private Instant scannedAt;

    @Embedded
    private ScanImage image;

    private String cropLabel;
    private String diseaseName;
    private boolean healthy;
    private int confidence;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private @Nullable Integer severityStage;
    private String severityLabel;
    private @Nullable String pathogen;
    private @Nullable String symptom;
    private @Nullable String finding;
    private @Nullable String note;

    @Column(length = 1000)
    private @Nullable String cause;

    private @Nullable String climateTrigger;
    private @Nullable String verifiedBy;
    private @Nullable String audioLanguages;
    private @Nullable Integer audioDurationSeconds;

    @Embedded
    private @Nullable Prescription prescription;

    @Embedded
    @AttributeOverride(name = "summary", column = @Column(name = "organic_summary"))
    @AttributeOverride(name = "note", column = @Column(name = "organic_note"))
    @AttributeOverride(name = "badge", column = @Column(name = "organic_badge"))
    private @Nullable OrganicAlternative organicAlternative;

    @ElementCollection
    @CollectionTable(name = "scan_sanitation_step", joinColumns = @JoinColumn(name = "scan_id"))
    @OrderColumn(name = "position")
    @Column(name = "step")
    private List<String> sanitationSteps = new ArrayList<>();

    private @Nullable String prescriptionShort;
    private int stockistCount;
    private @Nullable Long plotId;
    private @Nullable String plotLabel;
    private @Nullable Long treatmentPlanId;

    protected Scan() {
    }

    public Scan(Diagnosis diagnosis, ScanMode mode, Instant scannedAt, ScanImage image, int stockistCount) {
        DiseaseProfile profile = diagnosis.profile();
        this.crop = diagnosis.crop();
        this.mode = mode;
        this.scannedAt = scannedAt;
        this.image = image;
        this.stockistCount = stockistCount;
        this.cropLabel = profile.cropLabel();
        this.diseaseName = profile.diseaseName();
        this.healthy = profile.healthy();
        this.confidence = profile.confidence();
        this.severity = profile.severity();
        this.severityStage = profile.severityStage();
        this.severityLabel = profile.severityLabel();
        this.pathogen = profile.pathogen();
        this.symptom = profile.symptom();
        this.finding = profile.finding();
        this.note = profile.note();
        this.cause = profile.cause();
        this.climateTrigger = profile.climateTrigger();
        this.verifiedBy = profile.verifiedBy();
        this.audioLanguages = profile.audioLanguages();
        this.audioDurationSeconds = profile.audioDurationSeconds();
        this.prescription = profile.prescription();
        this.organicAlternative = profile.organicAlternative();
        this.sanitationSteps = new ArrayList<>(profile.sanitationSteps());
        this.prescriptionShort = profile.prescriptionShort();
    }

    /** Files the scan under the plot it was taken on, joining that plot's running treatment plan. */
    public void linkToPlot(PlotLink plot) {
        this.plotId = plot.plotId();
        this.plotLabel = plot.label();
        this.treatmentPlanId = plot.activeTreatmentPlanId();
    }

    public void assignTreatmentPlan(long planId) {
        this.treatmentPlanId = planId;
    }

    public Long getId() {
        return id;
    }

    public Crop getCrop() {
        return crop;
    }

    public ScanMode getMode() {
        return mode;
    }

    public Instant getScannedAt() {
        return scannedAt;
    }

    public ScanImage getImage() {
        return image;
    }

    public String getCropLabel() {
        return cropLabel;
    }

    public String getDiseaseName() {
        return diseaseName;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public int getConfidence() {
        return confidence;
    }

    public Severity getSeverity() {
        return severity;
    }

    public @Nullable Integer getSeverityStage() {
        return severityStage;
    }

    public String getSeverityLabel() {
        return severityLabel;
    }

    public @Nullable String getPathogen() {
        return pathogen;
    }

    public @Nullable String getSymptom() {
        return symptom;
    }

    public @Nullable String getFinding() {
        return finding;
    }

    public @Nullable String getNote() {
        return note;
    }

    public @Nullable String getCause() {
        return cause;
    }

    public @Nullable String getClimateTrigger() {
        return climateTrigger;
    }

    public @Nullable String getVerifiedBy() {
        return verifiedBy;
    }

    public @Nullable String getAudioLanguages() {
        return audioLanguages;
    }

    public @Nullable Integer getAudioDurationSeconds() {
        return audioDurationSeconds;
    }

    public @Nullable Prescription getPrescription() {
        return prescription;
    }

    public @Nullable OrganicAlternative getOrganicAlternative() {
        return organicAlternative;
    }

    public List<String> getSanitationSteps() {
        return List.copyOf(sanitationSteps);
    }

    public @Nullable String getPrescriptionShort() {
        return prescriptionShort;
    }

    public int getStockistCount() {
        return stockistCount;
    }

    public @Nullable Long getPlotId() {
        return plotId;
    }

    public @Nullable String getPlotLabel() {
        return plotLabel;
    }

    public @Nullable Long getTreatmentPlanId() {
        return treatmentPlanId;
    }
}
