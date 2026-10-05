package com.myagree.app.common;

import org.jspecify.annotations.Nullable;

import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * A PATCH-body field that tells "left out" apart from "sent as null", like {@code email?: string | null} in
 * frontend/src/lib/types.ts. Declare the field as {@code @Nullable Change<T>}:
 * <ul>
 *   <li>the field is {@code null} when the client left it out: keep the stored value;</li>
 *   <li>{@link #value()} is {@code null} when the client sent {@code null}: clear the stored value;</li>
 *   <li>otherwise {@link #value()} holds the new value.</li>
 * </ul>
 * Constraints go on the type argument and validate the new value, e.g.
 * {@code @Nullable Change<@Email String> email}.
 */
@JsonDeserialize(using = ChangeDeserializer.class)
public record Change<T>(@Nullable T value) {
}
