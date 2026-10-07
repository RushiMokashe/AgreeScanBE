package com.myagree.app.store;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.ClockConfig;
import com.myagree.app.common.spi.AdminMetric;
import com.myagree.app.common.spi.AdminMetricsContributor;

/** The admin overview's store number: orders placed today (India time). */
@Component
class StoreMetrics implements AdminMetricsContributor {

    private final OrderRepository orderRepository;
    private final Clock clock;

    StoreMetrics(OrderRepository orderRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<AdminMetric, Long> metrics() {
        ZonedDateTime today = LocalDate.ofInstant(clock.instant(), ClockConfig.FARM_ZONE).atStartOfDay(ClockConfig.FARM_ZONE);
        return Map.of(AdminMetric.ORDERS_TODAY,
                orderRepository.countPlacedBetween(today.toInstant(), today.plusDays(1).toInstant()));
    }
}
