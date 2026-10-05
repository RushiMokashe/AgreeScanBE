package com.myagree.app.weather;

import org.springframework.stereotype.Service;

/** Fixed readings for the demo farmer's district (Solapur); the location argument is ignored. */
@Service
class DemoWeatherService implements WeatherService {

    private static final WeatherReport SOLAPUR_WEATHER =
            new WeatherReport(29, "Partly Cloudy", "partly_cloudy_day", 68, 2);

    private static final FieldConditions SOLAPUR_FIELD_CONDITIONS =
            new FieldConditions(78, "Optimal for Fungal Check", 3, "Blight within 5 km");

    @Override
    public WeatherReport currentWeather(String location) {
        return SOLAPUR_WEATHER;
    }

    @Override
    public FieldConditions fieldConditions(String location) {
        return SOLAPUR_FIELD_CONDITIONS;
    }
}
