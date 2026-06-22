package com.adaptive.server.repository;

import com.adaptive.server.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findTop10ByOrderByTotalStarsDesc();

    // Admin analytics — role is a String column on User (default "STUDENT").
    long countByRole(String role);

    // Leaderboard scoped to a role, so admins (even ex-students) don't appear.
    List<User> findTop10ByRoleOrderByTotalStarsDesc(String role);

    // Guard: don't block the last ACTIVE admin.
    long countByRoleAndAccountStatus(String role, String accountStatus);

    // Admin user management: optional text search (fullName/email, plus exact id when
    // the search is numeric) and optional role filter, server-side paginated.
    @Query(value = "SELECT u FROM User u WHERE u.accountStatus IN :statuses AND " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:q IS NULL " +
            "  OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "  OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :q, '%')) " +
            "  OR (:idQ IS NOT NULL AND u.id = :idQ))",
           countQuery = "SELECT COUNT(u) FROM User u WHERE u.accountStatus IN :statuses AND " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:q IS NULL " +
            "  OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "  OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :q, '%')) " +
            "  OR (:idQ IS NOT NULL AND u.id = :idQ))")
    Page<User> searchUsers(@Param("q") String q, @Param("idQ") Long idQ,
                           @Param("role") String role,
                           @Param("statuses") Collection<String> statuses, Pageable pageable);

}
