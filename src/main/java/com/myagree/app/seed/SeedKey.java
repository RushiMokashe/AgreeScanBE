package com.myagree.app.seed;

/**
 * Names a record in the {@link SeedContext} and fixes its type, e.g.
 * {@code static final SeedKey<FieldPlots> PLOTS = SeedKey.named("field plots");}. Keys compare by identity, so
 * always use the constant declared by the seed that puts the record.
 */
public final class SeedKey<T> {

    private final String name;

    private SeedKey(String name) {
        this.name = name;
    }

    public static <T> SeedKey<T> named(String name) {
        return new SeedKey<>(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
