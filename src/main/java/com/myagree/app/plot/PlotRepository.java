package com.myagree.app.plot;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlotRepository extends JpaRepository<Plot, Long> {

    @EntityGraph(attributePaths = "metrics")
    List<Plot> findAllByOrderByIdAsc();

    Optional<Plot> findFirstByCropOrderByIdAsc(Crop crop);
}
