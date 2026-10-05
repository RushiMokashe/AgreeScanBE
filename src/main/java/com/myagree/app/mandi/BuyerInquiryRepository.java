package com.myagree.app.mandi;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerInquiryRepository extends JpaRepository<BuyerInquiry, Long> {

    @EntityGraph(attributePaths = "specs")
    List<BuyerInquiry> findByMarketOrderByIdAsc(MandiMarket market);

    @EntityGraph(attributePaths = "specs")
    Optional<BuyerInquiry> findWithSpecsById(long id);
}
