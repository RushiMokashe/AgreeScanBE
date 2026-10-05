package com.myagree.app.mandi;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommodityPriceRepository extends JpaRepository<CommodityPrice, Long> {

    List<CommodityPrice> findByMarketOrderByIdAsc(MandiMarket market);
}
