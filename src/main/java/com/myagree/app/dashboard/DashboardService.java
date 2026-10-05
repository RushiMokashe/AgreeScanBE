package com.myagree.app.dashboard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.dashboard.dto.DashboardResponse;
import com.myagree.app.dashboard.dto.HelplineResponse;
import com.myagree.app.farmer.FarmerService;
import com.myagree.app.farmer.dto.FarmerProfileResponse;
import com.myagree.app.plot.PlotService;
import com.myagree.app.scan.ScanService;
import com.myagree.app.weather.WeatherService;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** The home screen lists this many recent scans. */
    private static final int RECENT_SCAN_LIMIT = 5;

    /** Government of India's toll-free farmer helpline. */
    private static final HelplineResponse KISAN_CALL_CENTER =
            new HelplineResponse("Kisan Call Center", "18001801551", "1800-180-1551", "Toll-Free 24/7");

    private final FarmerService farmerService;
    private final WeatherService weatherService;
    private final PlotService plotService;
    private final ScanService scanService;
    private final RiskAlertRepository alertRepository;

    public DashboardService(FarmerService farmerService, WeatherService weatherService, PlotService plotService,
                            ScanService scanService, RiskAlertRepository alertRepository) {
        this.farmerService = farmerService;
        this.weatherService = weatherService;
        this.plotService = plotService;
        this.scanService = scanService;
        this.alertRepository = alertRepository;
    }

    /** Everything the home screen shows, in one call. */
    public DashboardResponse dashboard() {
        FarmerProfileResponse farmer = farmerService.currentFarmer();
        return DashboardMapper.toResponse(
                farmer,
                weatherService.currentWeather(farmer.location()),
                alertRepository.findAllByOrderByIdAsc(),
                plotService.listPlots(),
                scanService.recentScans(RECENT_SCAN_LIMIT),
                KISAN_CALL_CENTER);
    }
}
