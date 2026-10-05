package com.myagree.app.care.dto;

/** Mirrors {@code Agronomist} in frontend/src/lib/types.ts. */
public record AgronomistResponse(
        long id,
        String name,
        String title,
        String photoUrl,
        String phone,
        boolean onDuty,
        String pitch) {
}
