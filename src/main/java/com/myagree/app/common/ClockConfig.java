package com.myagree.app.common;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The single source of "now". Services and seeders take a {@link Clock} instead of calling
 * {@code Instant.now()}, so tests can pin time with a fixed clock.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    /** Farm days ("today", "yesterday 4:30 PM") are reckoned in India Standard Time. */
    public static final ZoneId FARM_ZONE = ZoneId.of("Asia/Kolkata");

    /** Ticks in whole seconds so API timestamps read like "2026-09-29T02:00:00Z". */
    @Bean
    Clock clock() {
        return Clock.tickSeconds(FARM_ZONE);
    }
}
