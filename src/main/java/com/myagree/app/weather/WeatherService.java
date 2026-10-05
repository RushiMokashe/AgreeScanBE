package com.myagree.app.weather;

/**
 * Seam for agro-meteorological data: current weather plus the field micro-climate and disease-pressure
 * readings that agromet advisories (such as IMD's Gramin Krishi Mausam Sewa bulletins) publish per district.
 *
 * <p>{@link DemoWeatherService} returns fixed Solapur readings. A live provider replaces it by registering
 * another {@code WeatherService} bean marked {@code @Primary}; callers do not change.
 */
public interface WeatherService {

    /** Current conditions at a farmer's location, e.g. "Solapur, Maharashtra". */
    WeatherReport currentWeather(String location);

    /** Field moisture and nearby disease outbreaks, shown on the crop scanner. */
    FieldConditions fieldConditions(String location);
}
