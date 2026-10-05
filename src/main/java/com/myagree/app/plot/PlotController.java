package com.myagree.app.plot;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.plot.dto.FieldPlotResponse;

@RestController
@RequestMapping("/api/plots")
class PlotController {

    private final PlotService plotService;

    PlotController(PlotService plotService) {
        this.plotService = plotService;
    }

    @GetMapping
    List<FieldPlotResponse> plots() {
        return plotService.listPlots();
    }
}
