package com.myagree.app.payment;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.ClockConfig;
import com.myagree.app.common.spi.AdminMetric;
import com.myagree.app.common.spi.AdminMetricsContributor;

/** The admin overview's revenue: rupees received online this month (India time). */
@Component
class PaymentMetrics implements AdminMetricsContributor {

    private final PaymentRepository repository;
    private final Clock clock;

    PaymentMetrics(PaymentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<AdminMetric, Long> metrics() {
        Instant monthStart = LocalDate.ofInstant(clock.instant(), ClockConfig.FARM_ZONE).withDayOfMonth(1)
                .atStartOfDay(ClockConfig.FARM_ZONE).toInstant();
        return Map.of(AdminMetric.REVENUE_THIS_MONTH, repository.sumSettledSince(PaymentStatusCode.SUCCEEDED, monthStart));
    }
}
