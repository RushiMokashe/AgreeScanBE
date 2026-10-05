package com.myagree.app.plot;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.NotFoundException;
import com.myagree.app.plot.dto.FieldPlotResponse;

@Service
@Transactional(readOnly = true)
public class PlotService {

    private final PlotRepository plotRepository;

    public PlotService(PlotRepository plotRepository) {
        this.plotRepository = plotRepository;
    }

    /** The farmer's field plots with their stat tiles, in plot order (A, B, C, ...). */
    public List<FieldPlotResponse> listPlots() {
        return plotRepository.findAllByOrderByIdAsc().stream()
                .map(PlotMapper::toResponse)
                .toList();
    }

    /** The plot where scans of {@code crop} are filed: the first plot growing it, if the farmer grows it at all. */
    public Optional<PlotLink> findPlotGrowing(Crop crop) {
        return plotRepository.findFirstByCropOrderByIdAsc(crop).map(PlotMapper::toLink);
    }

    /** Makes {@code scanId} the plot's most recent diagnosis. */
    @Transactional
    public void recordLatestScan(long plotId, long scanId) {
        plotRepository.findById(plotId)
                .orElseThrow(() -> NotFoundException.of("Plot", plotId))
                .recordLatestScan(scanId);
    }
}
