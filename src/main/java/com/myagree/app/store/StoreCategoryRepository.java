package com.myagree.app.store;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreCategoryRepository extends JpaRepository<StoreCategory, Long> {

    List<StoreCategory> findAllByOrderByIdAsc();
}
