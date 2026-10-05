package com.myagree.app.notification;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** One page of the user's notifications, newest first; ids order those stored within the same second. */
    Page<Notification> findByUserIdOrderByCreatedAtDescIdDesc(long userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(long userId);

    Optional<Notification> findByIdAndUserId(long id, long userId);

    /**
     * Marks all of the user's unread notifications read in one statement. Pending changes are written first and the
     * persistence context is cleared afterwards, so no stale notification is served later in the same transaction.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Notification n set n.readAt = :readAt where n.userId = :userId and n.readAt is null")
    void markAllRead(long userId, Instant readAt);
}
