package com.myagree.app.care;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AgroDealerRepository extends JpaRepository<AgroDealer, Long> {

    List<AgroDealer> findByDistanceKmLessThanEqualOrderByDistanceKmAsc(double maxDistanceKm);

    long countByDistanceKmLessThanEqual(double maxDistanceKm);
}
