package com.myagree.app.common;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

/** Colour intent of a UI label; serialized in lower case ("neutral", "success", ...) as the frontend expects. */
public enum Tone {
    NEUTRAL,
    SUCCESS,
    WARNING,
    ERROR;

    @JsonValue
    public String jsonValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
