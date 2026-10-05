package com.myagree.app.dashboard.dto;

import org.jspecify.annotations.Nullable;

/** Mirrors {@code WeatherSummary} in frontend/src/lib/types.ts. */
public record WeatherSummaryResponse(
        int temperatureC,
        String condition,
        String conditionIcon,
        int humidityPercent,
        @Nullable Integer rainInDays) {
}
