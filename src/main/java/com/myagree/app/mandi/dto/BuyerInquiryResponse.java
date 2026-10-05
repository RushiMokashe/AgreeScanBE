package com.myagree.app.mandi.dto;

import java.util.List;

import org.jspecify.annotations.Nullable;

/** Mirrors {@code BuyerInquiry} in frontend/src/lib/types.ts. */
public record BuyerInquiryResponse(
        long id,
        String buyerName,
        boolean verified,
        String subtitle,
        String icon,
        String badge,
        boolean badgeHighlighted,
        int volumeTonnes,
        String produce,
        String priceLabel,
        int pricePerQuintal,
        String priceNote,
        boolean priceNoteHighlighted,
        boolean highlighted,
        @Nullable String specsTitle,
        List<String> specs,
        String footerIcon,
        String footerText,
        String actionLabel,
        @Nullable String actionIcon,
        boolean actionPrimary,
        boolean responded) {
}
