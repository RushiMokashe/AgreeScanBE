package com.myagree.app.notification;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.myagree.app.common.spi.NotificationType;
import com.myagree.app.common.spi.Notifier;

/**
 * The {@link Notifier} of docs/architecture/phase-2.md, D10: notifications shown inside AgriScan, in the bell list and
 * live in the recipient's open app tabs. Each one is stored in the caller's transaction, so the list never misses it,
 * and pushed to the recipient's open streams only once that transaction has committed: a rolled-back change notifies
 * nobody, and a failed push is logged, never thrown at the caller.
 *
 * <p>Besides a missing parameter, a route that is not a path inside the app, such as "/bookings/7", is a programming
 * error reported with {@link IllegalArgumentException}, so a notification can never link away from the app. Parameter
 * values are kept on one line and cut to {@value #MAX_PARAMETER_LENGTH} characters, so no value, however long, can fail
 * the caller's transaction.
 */
@Component
class InAppNotifier implements Notifier {

    /** Longest parameter value kept; a typed decline or cancel reason may have up to 200 characters. */
    static final int MAX_PARAMETER_LENGTH = 200;

    /** One leading slash, not followed by a second slash or a backslash, which browsers would read as another site. */
    private static final Pattern APP_ROUTE = Pattern.compile("/(?![/\\\\])[^\\s\\p{Cntrl}]*");
    /** Line breaks, tabs, other control characters and runs of spaces, each shown as one space. */
    private static final Pattern SPACING = Pattern.compile("[\\s\\p{Cntrl}]+");
    private static final String SPACE = " ";
    private static final String ELLIPSIS = "…";

    private final NotificationRepository repository;
    private final NotificationMapper mapper;
    private final NotificationStreams streams;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    InAppNotifier(NotificationRepository repository, NotificationMapper mapper, NotificationStreams streams,
                  ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.streams = streams;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void notify(long userId, NotificationType type, Map<String, String> params, String route) {
        Notification notification = new Notification(userId, type, displayValues(type, params), requireAppRoute(route),
                clock.instant());
        events.publishEvent(new NotificationStored(repository.save(notification)));
    }

    /** Pushes a notification to its recipient's open streams once the transaction that stored it has committed. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void push(NotificationStored stored) {
        Notification notification = stored.notification();
        streams.push(notification.getUserId(), language -> mapper.toResponse(notification, language));
    }

    /** The type's parameters as one-line display text; keys the type does not use are left out. */
    private static Map<String, String> displayValues(NotificationType type, Map<String, String> params) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String parameter : type.parameters()) {
            if (!params.containsKey(parameter)) {
                throw new IllegalArgumentException(
                        "%s needs the parameter '%s', but got %s".formatted(type, parameter, params.keySet()));
            }
            values.put(parameter, oneLine(params.get(parameter)));
        }
        return values;
    }

    /** The value on one line, cut to {@value #MAX_PARAMETER_LENGTH} characters; {@code null} counts as empty. */
    private static String oneLine(@Nullable String value) {
        if (value == null) {
            return "";
        }
        String text = SPACING.matcher(value).replaceAll(SPACE).strip();
        if (text.length() <= MAX_PARAMETER_LENGTH) {
            return text;
        }
        int end = MAX_PARAMETER_LENGTH - ELLIPSIS.length();
        if (Character.isHighSurrogate(text.charAt(end - 1))) {
            end--; // never split a character stored as two chars, such as an emoji
        }
        return text.substring(0, end).stripTrailing() + ELLIPSIS;
    }

    private static String requireAppRoute(String route) {
        if (route.length() > Notification.ROUTE_MAX_LENGTH || !APP_ROUTE.matcher(route).matches()) {
            throw new IllegalArgumentException(
                    "Route '%s' is not a path inside the app, such as \"/bookings/7\"".formatted(route));
        }
        return route;
    }

    /** A notification was stored; {@link #push} delivers it once the transaction commits. */
    record NotificationStored(Notification notification) {
    }
}
