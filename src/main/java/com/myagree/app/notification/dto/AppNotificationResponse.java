package com.myagree.app.notification.dto;

import java.time.Instant;

import com.myagree.app.common.spi.NotificationType;

/**
 * Mirrors {@code AppNotification} in frontend/src/lib/types.ts.
 *
 * @param title in the reader's language, e.g. "New booking request"
 * @param body  in the reader's language, e.g. "Rishikesh wants Mahindra 575 DI for Slot: 02:00 PM Today (₹1,300)"
 * @param route the app screen to open, e.g. "/owner/bookings/7"
 */
public record AppNotificationResponse(
        long id,
        NotificationType type,
        String title,
        String body,
        String route,
        boolean read,
        Instant createdAt) {
}
