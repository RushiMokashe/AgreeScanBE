package com.myagree.app.common.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.EmbeddedColumnNaming;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.myagree.app.support.AgriScanApiTest;

/** The column mapping {@link LocalizedText} documents for entities, element collections and optional texts. */
@AgriScanApiTest
class LocalizedTextMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void everyTextGetsFieldNamedColumnsOfTheFullLength() {
        assertThat(columnLengths("LOCALIZED_TEXT_SAMPLE")).containsAllEntriesOf(Map.of(
                "TITLE_EN", LocalizedText.MAX_LENGTH, "TITLE_MR", LocalizedText.MAX_LENGTH,
                "TITLE_HI", LocalizedText.MAX_LENGTH, "RATE_NOTE_EN", LocalizedText.MAX_LENGTH));
        assertThat(columnLengths("LOCALIZED_TEXT_SAMPLE_SPEC")).containsKeys(
                "VALUE_EN", "VALUE_MR", "VALUE_HI", "LABEL_EN", "LABEL_MR", "LABEL_HI");
    }

    @Test
    void textsSurviveARoundTripAndAnAbsentOptionalTextStaysAbsent() {
        LocalizedText title = LocalizedText.of("Rotavator", "रोटाव्हेटर", null);
        Sample sample = new Sample(title, null, List.of(new Spec(LocalizedText.of("6 Feet", null, null),
                LocalizedText.of("Rotavator Width", "रोटाव्हेटर रुंदी", "रोटावेटर चौड़ाई"))));
        entityManager.persist(sample);
        entityManager.flush();
        entityManager.clear();

        Sample loaded = entityManager.find(Sample.class, sample.id);

        assertThat(loaded.title).isEqualTo(title);
        assertThat(loaded.title.resolve(Language.HI)).isEqualTo("Rotavator");
        assertThat(loaded.rateNote).isNull();
        assertThat(loaded.specs).singleElement()
                .satisfies(spec -> assertThat(spec.label().resolve(Language.MR)).isEqualTo("रोटाव्हेटर रुंदी"));
    }

    private Map<String, Integer> columnLengths(String table) {
        List<?> rows = entityManager.createNativeQuery("""
                        select column_name, character_maximum_length from information_schema.columns
                        where table_name = :table""")
                .setParameter("table", table)
                .getResultList();
        return rows.stream()
                .map(Object[].class::cast)
                .collect(Collectors.toMap(row -> (String) row[0], row -> row[1] == null ? 0 : ((Number) row[1]).intValue()));
    }

    /** An entity as features map one: a required text, an optional text and texts in an element collection. */
    @Entity(name = "LocalizedTextSample")
    @Table(name = "localized_text_sample")
    static class Sample {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Embedded
        @EmbeddedColumnNaming("title_%s")
        private LocalizedText title;

        @Embedded
        @EmbeddedColumnNaming("rate_note_%s")
        private @Nullable LocalizedText rateNote;

        @ElementCollection
        @CollectionTable(name = "localized_text_sample_spec", joinColumns = @JoinColumn(name = "sample_id"))
        private List<Spec> specs = new ArrayList<>();

        protected Sample() {
        }

        Sample(LocalizedText title, @Nullable LocalizedText rateNote, List<Spec> specs) {
            this.title = title;
            this.rateNote = rateNote;
            this.specs = new ArrayList<>(specs);
        }
    }

    @Embeddable
    record Spec(@EmbeddedColumnNaming("value_%s") LocalizedText value,
                @EmbeddedColumnNaming("label_%s") LocalizedText label) {
    }
}
