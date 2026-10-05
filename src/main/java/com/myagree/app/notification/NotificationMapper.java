package com.myagree.app.notification;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.myagree.app.common.i18n.IndianNumbers;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.Messages;
import com.myagree.app.common.spi.NotificationType;
import com.myagree.app.common.spi.Notifier;
import com.myagree.app.notification.dto.AppNotificationResponse;

/**
 * Shows a notification to its reader. The title and body are rendered whenever the notification is read, in the
 * reader's language, from the messages {@code notification.<TYPE>.title} and {@code notification.<TYPE>.body} of
 * i18n/notification*.properties, whose placeholders are the type's parameters in order ({@link NotificationType}).
 * A type with a reason uses {@code notification.<TYPE>.body.no-reason} when none was given. Amounts show as rupees in
 * Indian grouping ("1300" as "₹1,300"), and a parameter the notification lacks shows as empty text instead of failing
 * the whole list.
 */
@Component
class NotificationMapper {

    private static final String CODE_PREFIX = "notification.";
    private static final String TITLE = ".title";
    private static final String BODY = ".body";
    private static final String BODY_WITHOUT_REASON = ".body.no-reason";
    private static final String MISSING_VALUE = "";

    private final Messages messages;

    NotificationMapper(Messages messages) {
        this.messages = messages;
    }

    AppNotificationResponse toResponse(Notification notification, Language language) {
        NotificationType type = notification.getType();
        Map<String, String> params = notification.getParams();
        Object[] arguments = type.parameters().stream()
                .map(parameter -> displayValue(parameter, params.getOrDefault(parameter, MISSING_VALUE)))
                .toArray();
        return new AppNotificationResponse(
                notification.getId(),
                type,
                messages.get(CODE_PREFIX + type.name() + TITLE, language, arguments),
                messages.get(CODE_PREFIX + type.name() + bodyVariant(type, params), language, arguments),
                notification.getRoute(),
                notification.isRead(),
                notification.getCreatedAt());
    }

    private static String displayValue(String parameter, String value) {
        return Notifier.AMOUNT.equals(parameter) ? rupees(value) : value;
    }

    /** Whole rupees in plain digits, as the {@link Notifier} contract asks, in Indian grouping; anything else as given. */
    private static String rupees(String amount) {
        try {
            return IndianNumbers.rupees(Long.parseLong(amount.strip()));
        } catch (NumberFormatException notWholeRupees) {
            return amount;
        }
    }

    private static String bodyVariant(NotificationType type, Map<String, String> params) {
        boolean reasonLeftOut = type.parameters().contains(Notifier.REASON)
                && params.getOrDefault(Notifier.REASON, MISSING_VALUE).isBlank();
        return reasonLeftOut ? BODY_WITHOUT_REASON : BODY;
    }
}
