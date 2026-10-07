package com.myagree.app.notification;

import java.time.Clock;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.myagree.app.common.NotFoundException;
import com.myagree.app.common.PageResponse;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.notification.dto.AppNotificationResponse;
import com.myagree.app.notification.dto.UnreadCountResponse;

/**
 * A signed-in user's notifications (docs/architecture/phase-2.md, D10): the bell list, its unread badge and the live
 * stream. Every method is scoped to the given account; another user's notification answers 404, so ids never leak.
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    /** The longest page a client may ask for. */
    public static final int MAX_PAGE_SIZE = 50;

    private static final String NOT_FOUND = "notification.not-found";

    private final NotificationRepository repository;
    private final NotificationMapper mapper;
    private final NotificationStreams streams;
    private final Clock clock;

    NotificationService(NotificationRepository repository, NotificationMapper mapper, NotificationStreams streams,
                        Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.streams = streams;
        this.clock = clock;
    }

    /** One page of the user's notifications, newest first, rendered in {@code language}. */
    public PageResponse<AppNotificationResponse> list(long userId, int page, int size, Language language) {
        return PageResponse.of(repository.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(page, size))
                .map(notification -> mapper.toResponse(notification, language)));
    }

    /** The number on the bell's badge. */
    public UnreadCountResponse unreadCount(long userId) {
        return new UnreadCountResponse(repository.countByUserIdAndReadAtIsNull(userId));
    }

    /**
     * Marks one of the user's notifications read; reading it again keeps the first read time.
     *
     * @throws NotFoundException when the user has no such notification
     */
    @Transactional
    public AppNotificationResponse markRead(long userId, long notificationId, Language language) {
        Notification notification = repository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException(UserMessage.of(NOT_FOUND, String.valueOf(notificationId))));
        notification.markRead(clock.instant());
        return mapper.toResponse(notification, language);
    }

    /** Marks every unread notification of the user read. */
    @Transactional
    public void markAllRead(long userId) {
        repository.markAllRead(userId, clock.instant());
    }

    /**
     * Opens a live stream of the user's new notifications, shown in {@code language}.
     *
     * @throws StreamUnavailableException while too many streams are open, or during shutdown (HTTP 503)
     */
    public SseEmitter openStream(long userId, Language language) {
        return streams.open(userId, language);
    }
}
