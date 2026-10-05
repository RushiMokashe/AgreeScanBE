package com.myagree.app.store;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** The farmer's orders with their lines, newest first. */
    @EntityGraph(attributePaths = "lines")
    List<Order> findByFarmerIdOrderByPlacedAtDescIdDesc(long farmerId);

    /** One of the farmer's orders with its lines; empty when it belongs to another farmer. */
    @EntityGraph(attributePaths = "lines")
    Optional<Order> findWithLinesByIdAndFarmerId(long id, long farmerId);

    /** One of the farmer's orders without its lines; empty when it belongs to another farmer. */
    Optional<Order> findByIdAndFarmerId(long id, long farmerId);

    /** Orders placed from {@code from} (inclusive) until {@code until} (exclusive). */
    @Query("select count(o) from Order o where o.placedAt >= :from and o.placedAt < :until")
    long countPlacedBetween(Instant from, Instant until);
}
