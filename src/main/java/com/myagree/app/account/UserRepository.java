package com.myagree.app.account;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.myagree.app.common.SearchPatterns;
import com.myagree.app.common.security.Role;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    /**
     * Users holding {@code role} whose name or phone number matches {@code pattern}; a {@code null} argument disables
     * that filter. Build the pattern with {@link SearchPatterns#containsIgnoringCase(String)}.
     */
    @Query("""
            select u from User u
            where (:role is null or :role member of u.roles)
              and (:pattern is null or lower(u.name) like :pattern escape '!' or u.phone like :pattern escape '!')""")
    Page<User> search(@Nullable String pattern, @Nullable Role role, Pageable pageable);

    /** How many users hold each role; roles nobody holds are left out. */
    @Query("select new com.myagree.app.account.RoleCount(r, count(u)) from User u join u.roles r group by r")
    List<RoleCount> countUsersByRole();
}
