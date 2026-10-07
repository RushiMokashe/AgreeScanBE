package com.myagree.app.store;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    Optional<Shop> findByOwnerUserId(long ownerUserId);

    /** The shop whose current UPI QR is this file; a replaced QR is no longer served. */
    Optional<Shop> findByUpiQrFileName(String fileName);
}
