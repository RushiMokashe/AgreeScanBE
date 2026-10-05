package com.myagree.app.farmer.dto;

/** Mirrors {@code FarmerProfile} in frontend/src/lib/types.ts. */
public record FarmerProfileResponse(long id, String name, String location, String season, String avatarUrl,
                                    FarmerPreferencesResponse preferences) {
}
