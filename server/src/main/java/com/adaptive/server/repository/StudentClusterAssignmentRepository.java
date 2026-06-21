package com.adaptive.server.repository;

import com.adaptive.server.entity.StudentClusterAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentClusterAssignmentRepository extends JpaRepository<StudentClusterAssignment, Long> {
    // Used both for upserting after a clustering run and (later) by the question
    // generator to read a student's cluster.
    Optional<StudentClusterAssignment> findByUserId(Long userId);

    // Admin ML Groups (read-only): one row per cluster, scoped to users whose CURRENT
    // role is :role. Joined to User by userId (userId is a plain Long, not an association),
    // so assignments belonging to users who are no longer students are excluded.
    // label/topErrors/avgAccuracy/modelK are denormalized per row, so grouping by them is safe.
    @Query("SELECT s.clusterId AS clusterId, s.clusterLabel AS label, s.clusterTopErrors AS topErrors, " +
           "s.clusterAvgAccuracy AS avgAccuracy, s.modelK AS modelK, " +
           "COUNT(s) AS memberCount, MAX(s.assignedAt) AS lastAssignedAt " +
           "FROM StudentClusterAssignment s, User u " +
           "WHERE u.id = s.userId AND u.role = :role " +
           "GROUP BY s.clusterId, s.clusterLabel, s.clusterTopErrors, s.clusterAvgAccuracy, s.modelK " +
           "ORDER BY s.clusterId ASC")
    List<AdminClusterGroupProjection> findClusterGroupsForRole(@Param("role") String role);

    @Query("SELECT MAX(s.assignedAt) FROM StudentClusterAssignment s, User u " +
           "WHERE u.id = s.userId AND u.role = :role")
    LocalDateTime findLastRunAtForRole(@Param("role") String role);

    @Query("SELECT COUNT(s) FROM StudentClusterAssignment s, User u " +
           "WHERE u.id = s.userId AND u.role = :role")
    long countAssignedForRole(@Param("role") String role);
}
