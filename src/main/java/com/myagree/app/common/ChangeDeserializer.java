package com.myagree.app.common;

import org.jspecify.annotations.Nullable;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;

/**
 * Reads a {@link Change}: a missing property stays {@code null}, an explicit JSON {@code null} becomes an
 * empty change, and any other value is read with the deserializer of the type argument.
 */
final class ChangeDeserializer extends ValueDeserializer<Change<?>> {

    /** Reads the new value; set once Jackson has contextualized this deserializer for a property. */
    private final @Nullable ValueDeserializer<Object> valueDeserializer;

    /** Used by {@link tools.jackson.databind.annotation.JsonDeserialize#using()}. */
    ChangeDeserializer() {
        this(null);
    }

    private ChangeDeserializer(@Nullable ValueDeserializer<Object> valueDeserializer) {
        this.valueDeserializer = valueDeserializer;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext context, @Nullable BeanProperty property) {
        JavaType changeType = property != null ? property.getType() : context.getContextualType();
        return new ChangeDeserializer(context.findContextualValueDeserializer(changeType.containedTypeOrUnknown(0), property));
    }

    @Override
    public Change<?> deserialize(JsonParser parser, DeserializationContext context) {
        if (valueDeserializer == null) {
            throw new IllegalStateException("Change must be read through a typed property, e.g. Change<String>");
        }
        return new Change<>(valueDeserializer.deserialize(parser, context));
    }

    /** The client sent {@code null}: clear the value. */
    @Override
    public Object getNullValue(DeserializationContext context) {
        return new Change<>(null);
    }

    /** The client left the property out: keep the value. */
    @Override
    public @Nullable Object getAbsentValue(DeserializationContext context) {
        return null;
    }
}
