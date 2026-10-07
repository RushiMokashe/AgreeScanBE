package com.myagree.app.notification;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.myagree.app.common.PageResponse;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.notification.dto.AppNotificationResponse;
import com.myagree.app.notification.dto.UnreadCountResponse;

/** The signed-in user's notifications, whatever their roles. */
@RestController
@RequestMapping("/api/notifications")
class NotificationController {

    private static final String FIRST_PAGE = "0";
    private static final String DEFAULT_PAGE_SIZE = "20";

    private final NotificationService notificationService;

    NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    PageResponse<AppNotificationResponse> list(CurrentUser user, Language language,
                                               @RequestParam(defaultValue = FIRST_PAGE) @Min(0) int page,
                                               @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1)
                                               @Max(NotificationService.MAX_PAGE_SIZE) int size) {
        return notificationService.list(user.userId(), page, size, language);
    }

    @GetMapping("/unread-count")
    UnreadCountResponse unreadCount(CurrentUser user) {
        return notificationService.unreadCount(user.userId());
    }

    @PostMapping("/{id}/read")
    AppNotificationResponse markRead(CurrentUser user, @PathVariable long id, Language language) {
        return notificationService.markRead(user.userId(), id, language);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markAllRead(CurrentUser user) {
        notificationService.markAllRead(user.userId());
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(CurrentUser user, Language language) {
        return notificationService.openStream(user.userId(), language);
    }
}
