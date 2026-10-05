package com.myagree.app.seed;

import org.springframework.stereotype.Component;

import com.myagree.app.dashboard.AlertSeverity;
import com.myagree.app.dashboard.RiskAlert;
import com.myagree.app.dashboard.RiskAlertRepository;

/** The regional risk alert on the home screen. */
@Component
class DashboardSeed implements DemoSeed {

    private final RiskAlertRepository alertRepository;

    DashboardSeed(RiskAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public void seed(SeedContext context) {
        alertRepository.save(new RiskAlert(
                "High Blight Risk in Karmala Taluka",
                "Heavy morning mist creates ideal spore conditions for Tomato crops. Inspect underside of leaves today.",
                AlertSeverity.HIGH,
                "Karmala Taluka"));
    }
}
