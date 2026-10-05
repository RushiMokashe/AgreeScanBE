package com.myagree.app.support;

import java.time.Clock;
import java.time.Instant;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.myagree.app.common.ClockConfig;

/** Pins "now" so seeded timestamps and derived labels are deterministic. */
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockConfiguration {

    /** 10:00 AM IST on 29 Sep 2026, after the seeded 07:30 AM spray. */
    public static final Instant NOW = Instant.parse("2026-09-29T04:30:00Z");

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW, ClockConfig.FARM_ZONE);
    }
}
