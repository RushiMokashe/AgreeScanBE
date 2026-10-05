package com.myagree.app.dashboard.dto;

import java.util.List;

import com.myagree.app.farmer.dto.FarmerProfileResponse;
import com.myagree.app.plot.dto.FieldPlotResponse;
import com.myagree.app.scan.dto.ScanSummaryResponse;

/** Mirrors {@code DashboardResponse} in frontend/src/lib/types.ts. */
public record DashboardResponse(
        FarmerProfileResponse farmer,
        WeatherSummaryResponse weather,
        List<RiskAlertResponse> alerts,
        List<FieldPlotResponse> plots,
        List<ScanSummaryResponse> recentScans,
        HelplineResponse helpline) {
}
