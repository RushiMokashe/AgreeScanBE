package com.myagree.app.common;

import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.ValueExtractor;

/**
 * Lets Bean Validation check the new value of a {@link Change}, so {@code Change<@Email String>} works like
 * {@code Optional<@Email String>}. Registered in META-INF/services/jakarta.validation.valueextraction.ValueExtractor.
 */
public final class ChangeValueExtractor implements ValueExtractor<Change<@ExtractedValue ?>> {

    @Override
    public void extractValues(Change<?> originalValue, ValueReceiver receiver) {
        receiver.value(null, originalValue.value());
    }
}
