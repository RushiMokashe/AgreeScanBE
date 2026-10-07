package com.myagree.app.rental.dto;

import org.jspecify.annotations.Nullable;

/** Mirrors {@code VehicleOwnerProfile} in frontend/src/lib/types.ts; the phone is the account's 10-digit number. */
public record VehicleOwnerProfileResponse(
        long id,
        long userId,
        String name,
        @Nullable String businessName,
        String phone,
        long hubId,
        String hubName) {
}
