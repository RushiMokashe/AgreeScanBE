package com.myagree.app.treatment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TreatmentMapperTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 29);

    @ParameterizedTest(name = "{0} step {1} day(s) from today -> \"{2}\"")
    @CsvSource({
            "COMPLETED, 0, Today",
            "UPCOMING,  0, Today",
            "UPCOMING,  1, In 1 Day",
            "UPCOMING,  3, In 3 Days",
            "SCHEDULED, 6, Scheduled",
            "UPCOMING, -1, 1 Day Overdue",
            "SCHEDULED, -2, 2 Days Overdue",
            "COMPLETED, -1, Yesterday",
            "COMPLETED, -3, 3 Days Ago",
            "COMPLETED,  2, In 2 Days"
    })
    void dayLabelDescribesTheStepDateRelativeToToday(StepStatus status, int daysFromToday, String expectedLabel) {
        String label = TreatmentMapper.dayLabel(status, TODAY.plusDays(daysFromToday), TODAY);

        assertThat(label).isEqualTo(expectedLabel);
    }
}
