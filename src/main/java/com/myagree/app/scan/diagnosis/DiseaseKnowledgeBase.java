package com.myagree.app.scan.diagnosis;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.myagree.app.plot.Crop;

import tools.jackson.databind.json.JsonMapper;

/**
 * Agronomist-curated disease profiles per crop, loaded from {@value #RULES_RESOURCE}. Rules are matched
 * in file order, so mode-specific rules must precede a crop's catch-all rule.
 */
@Component
public class DiseaseKnowledgeBase {

    static final String RULES_RESOURCE = "diagnosis/disease-knowledge-base.json";

    private final List<DiagnosisRule> rules;

    public DiseaseKnowledgeBase(JsonMapper jsonMapper) {
        this.rules = load(jsonMapper);
        requireEveryCropAndModeCovered(rules);
    }

    /** The profile reported for {@code crop} photographed in {@code mode}. */
    public DiseaseProfile profileFor(Crop crop, ScanMode mode) {
        return rules.stream()
                .filter(rule -> rule.matches(crop, mode))
                .findFirst()
                .map(DiagnosisRule::profile)
                .orElseThrow(() -> missingRule(crop, mode));
    }

    private static List<DiagnosisRule> load(JsonMapper jsonMapper) {
        try (InputStream json = new ClassPathResource(RULES_RESOURCE).getInputStream()) {
            return List.of(jsonMapper.readValue(json, DiagnosisRule[].class));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + RULES_RESOURCE, e);
        }
    }

    private static void requireEveryCropAndModeCovered(List<DiagnosisRule> rules) {
        for (Crop crop : Crop.values()) {
            for (ScanMode mode : ScanMode.values()) {
                if (rules.stream().noneMatch(rule -> rule.matches(crop, mode))) {
                    throw missingRule(crop, mode);
                }
            }
        }
    }

    private static IllegalStateException missingRule(Crop crop, ScanMode mode) {
        return new IllegalStateException("%s has no rule for %s scans of %s".formatted(RULES_RESOURCE, mode, crop));
    }
}
