package com.myagree.app.notification;

import java.time.Instant;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.spi.NotificationType;

/**
 * Something that happened to one of a user's bookings or payments, told to that user (docs/architecture/phase-2.md,
 * D10). Only the type, its parameters and the route are stored: the title and body are rendered in the reader's
 * language whenever the notification is read. Everything but the read time is fixed once stored.
 */
@Entity
@Table(name = "notification", indexes = {
        @Index(name = "notification_newest_by_user", columnList = "user_id, created_at, id"),
        @Index(name = "notification_unread_by_user", columnList = "user_id, read_at")})
class Notification {

    /**
     * Room for the parameters of any type as a JSON object: at most four values of
     * {@value InAppNotifier#MAX_PARAMETER_LENGTH} characters each, even if JSON has to escape every one of them.
     */
    static final int PARAMS_MAX_LENGTH = 2_000;
    static final int ROUTE_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The recipient's account; notifications belong to accounts, whatever their roles. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private NotificationType type;

    @Convert(converter = NotificationParamsConverter.class)
    @Column(nullable = false, updatable = false, length = PARAMS_MAX_LENGTH)
    private Map<String, String> params;

    @Column(nullable = false, updatable = false, length = ROUTE_MAX_LENGTH)
    private String route;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private @Nullable Instant readAt;

    protected Notification() {
    }

    /**
     * @param params a value for each of {@code type}'s parameters
     * @param route  the app screen with the details, e.g. "/bookings/7"
     */
    Notification(long userId, NotificationType type, Map<String, String> params, String route, Instant createdAt) {
        this.userId = userId;
        this.type = type;
        this.params = Map.copyOf(params);
        this.route = route;
        this.createdAt = createdAt;
    }

    /** Marks the notification read; one that was read before keeps its first read time. */
    void markRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    boolean isRead() {
        return readAt != null;
    }

    Long getId() {
        return id;
    }

    long getUserId() {
        return userId;
    }

    NotificationType getType() {
        return type;
    }

    Map<String, String> getParams() {
        return params;
    }

    String getRoute() {
        return route;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
