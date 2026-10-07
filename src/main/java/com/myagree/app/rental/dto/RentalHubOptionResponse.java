package com.myagree.app.rental.dto;

/** Mirrors {@code RentalHubOption} in frontend/src/lib/types.ts. */
public record RentalHubOptionResponse(long id, String name, int radiusKm, String routes) {
}
