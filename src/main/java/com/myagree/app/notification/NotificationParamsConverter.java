package com.myagree.app.notification;

import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stores a notification's parameters as one JSON object, e.g. {@code {"farmerName":"Rishikesh","amount":"1300"}}, so a
 * page of notifications loads in one query. It uses Jackson's default mapper rather than the web one, so the stored
 * format never changes with the API's JSON settings.
 */
@Converter
class NotificationParamsConverter implements AttributeConverter<Map<String, String>, String> {

    private static final TypeReference<Map<String, String>> PARAMS = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(Map<String, String> params) {
        return JsonMapper.shared().writeValueAsString(params);
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String json) {
        return Map.copyOf(JsonMapper.shared().readValue(json, PARAMS));
    }
}
