package com.myagree.app.rental;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RentalListingRepository extends JpaRepository<RentalListing, Long> {

    /** What farmers can book at a hub, in catalogue order. */
    @EntityGraph(attributePaths = {"owner", "specs", "features"})
    @Query("select l from RentalListing l where l.hub.id = :hubId and l.online = true and l.removed = false order by l.id")
    List<RentalListing> findBookableInHub(long hubId);

    /** A listing farmers can see, with its card's details. */
    @EntityGraph(attributePaths = {"owner", "specs", "features"})
    Optional<RentalListing> findWithDetailsByIdAndRemovedFalse(long id);

    /** An owner's vehicles, oldest first. */
    @EntityGraph(attributePaths = {"specs", "features"})
    List<RentalListing> findByOwnerIdAndRemovedFalseOrderByIdAsc(long ownerId);

    /** One of the owner's vehicles; another owner's id finds nothing. */
    @EntityGraph(attributePaths = {"specs", "features"})
    Optional<RentalListing> findByIdAndOwnerIdAndRemovedFalse(long id, long ownerId);

    /** Every vehicle on AgriScan, for the admin's rate list. */
    @EntityGraph(attributePaths = {"owner", "hub"})
    List<RentalListing> findByRemovedFalseOrderByIdAsc();

    @EntityGraph(attributePaths = {"owner", "hub"})
    Optional<RentalListing> findWithOwnerAndHubByIdAndRemovedFalse(long id);

    /** The listing showing an uploaded photo, for its signed media URL. */
    Optional<RentalListing> findByPhotoFileName(String fileName);

    long countByOwnerIdAndRemovedFalse(long ownerId);

    long countByOwnerIdAndOnlineTrueAndRemovedFalse(long ownerId);

    long countByOnlineTrueAndRemovedFalse();
}
