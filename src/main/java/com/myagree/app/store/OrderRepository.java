package com.myagree.app.store;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
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

    /** The shop's orders with their lines, newest first; {@code status} {@code null} for every status. */
    @EntityGraph(attributePaths = "lines")
    @Query("""
            select o from Order o
            where o.shopId = :shopId and (:status is null or o.status = :status)
            order by o.placedAt desc, o.id desc""")
    List<Order> findForShop(long shopId, @Nullable OrderStatus status, Pageable pageable);

    long countByShopIdAndStatus(long shopId, OrderStatus status);

    long countByShopIdAndPlacedAtGreaterThanEqual(long shopId, Instant from);

    /** Rupees of the shop's orders placed from {@code from}, cancelled ones aside. */
    @Query("""
            select coalesce(sum(o.total), 0) from Order o
            where o.shopId = :shopId and o.placedAt >= :from and o.status <> :cancelled""")
    long sumTotalForShopSince(long shopId, Instant from, OrderStatus cancelled);

    /** Orders placed from {@code from} (inclusive) until {@code until} (exclusive). */
    @Query("select count(o) from Order o where o.placedAt >= :from and o.placedAt < :until")
    long countPlacedBetween(Instant from, Instant until);
}
