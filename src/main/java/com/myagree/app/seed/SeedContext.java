package com.myagree.app.seed;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Records the demo seeds of one run share, by typed {@link SeedKey}. */
public final class SeedContext {

    private final Map<SeedKey<?>, Object> records = new HashMap<>();

    SeedContext() {
    }

    /**
     * @throws IllegalStateException when another seed already put a record under {@code key}
     */
    public <T> void put(SeedKey<T> key, T record) {
        if (records.putIfAbsent(key, Objects.requireNonNull(record, "record")) != null) {
            throw new IllegalStateException("The demo seed record '%s' was already put".formatted(key));
        }
    }

    /**
     * @throws IllegalStateException when no seed has put a record under {@code key} yet, i.e. the seed reading it
     *                               runs before the seed putting it
     */
    @SuppressWarnings("unchecked") // put() only accepts records of the key's type
    public <T> T get(SeedKey<T> key) {
        Object record = records.get(key);
        if (record == null) {
            throw new IllegalStateException(
                    "No demo seed has put '%s' yet; give the seed reading it a higher order()".formatted(key));
        }
        return (T) record;
    }
}
