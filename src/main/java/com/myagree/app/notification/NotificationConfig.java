package com.myagree.app.notification;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Runs the notification streams' heartbeat ({@link NotificationStreams#heartbeat()}). */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class NotificationConfig {
}
