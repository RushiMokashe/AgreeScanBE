package com.myagree.app.weather;

import org.jspecify.annotations.Nullable;

/**
 * @param conditionIcon Material Symbols icon name, e.g. "partly_cloudy_day"
 * @param rainInDays    days until rain is forecast, or {@code null} when none is expected
 */
public record WeatherReport(
        int temperatureC,
        String condition,
        String conditionIcon,
        int humidityPercent,
        @Nullable Integer rainInDays) {
}
