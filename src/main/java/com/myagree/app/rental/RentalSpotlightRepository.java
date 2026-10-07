package com.myagree.app.rental;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RentalSpotlightRepository extends JpaRepository<RentalSpotlight, Long> {

    /** The hub's spotlight offer while its listing takes bookings. */
    @Query("""
            select s from RentalSpotlight s join fetch s.listing l join fetch l.owner
            where l.hub.id = :hubId and l.online = true and l.removed = false""")
    Optional<RentalSpotlight> findBookableInHub(long hubId);
}
