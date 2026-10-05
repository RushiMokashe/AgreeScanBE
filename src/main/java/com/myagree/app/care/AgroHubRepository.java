package com.myagree.app.care;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AgroHubRepository extends JpaRepository<AgroHub, Long> {

    Optional<AgroHub> findFirstByOrderByIdAsc();
}
