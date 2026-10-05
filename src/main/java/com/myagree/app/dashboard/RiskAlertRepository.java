package com.myagree.app.dashboard;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {

    List<RiskAlert> findAllByOrderByIdAsc();
}
