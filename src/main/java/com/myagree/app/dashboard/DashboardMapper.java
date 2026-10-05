package com.myagree.app.dashboard;

import java.util.List;

import com.myagree.app.dashboard.dto.DashboardResponse;
import com.myagree.app.dashboard.dto.HelplineResponse;
import com.myagree.app.dashboard.dto.RiskAlertResponse;
import com.myagree.app.dashboard.dto.WeatherSummaryResponse;
import com.myagree.app.farmer.dto.FarmerProfileResponse;
import com.myagree.app.plot.dto.FieldPlotResponse;
import com.myagree.app.scan.dto.ScanSummaryResponse;
import com.myagree.app.weather.WeatherReport;

final class DashboardMapper {

    private DashboardMapper() {
    }

    static DashboardResponse toResponse(FarmerProfileResponse farmer, WeatherReport weather, List<RiskAlert> alerts,
                                        List<FieldPlotResponse> plots, List<ScanSummaryResponse> recentScans,
                                        HelplineResponse helpline) {
        return new DashboardResponse(
                farmer,
                toResponse(weather),
                alerts.stream().map(DashboardMapper::toResponse).toList(),
                plots,
                recentScans,
                helpline);
    }

    private static RiskAlertResponse toResponse(RiskAlert alert) {
        return new RiskAlertResponse(
                alert.getId(), alert.getTitle(), alert.getMessage(), alert.getSeverity(), alert.getRegion());
    }

    private static WeatherSummaryResponse toResponse(WeatherReport weather) {
        return new WeatherSummaryResponse(
                weather.temperatureC(),
                weather.condition(),
                weather.conditionIcon(),
                weather.humidityPercent(),
                weather.rainInDays());
    }
}
