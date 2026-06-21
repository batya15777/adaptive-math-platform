package com.adaptive.server.repository;

import com.adaptive.server.entity.StudentClusterAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentClusterAssignmentRepository extends JpaRepository<StudentClusterAssignment, Long> {
    // Used both for upserting after a clustering run and (later) by the question
    // generator to read a student's cluster.
    Optional<StudentClusterAssignment> findByUserId(Long userId);

    // Admin ML Groups (read-only): one row per cluster. The label/topErrors/avgAccuracy/
    // modelK columns are denormalized per row, so grouping by them is safe (all members
    // of a cluster from the same run share identical values).
    @Query("SELECT s.clusterId AS clusterId, s.clusterLabel AS label, s.clusterTopErrors AS topErrors, " +
           "s.clusterAvgAccuracy AS avgAccuracy, s.modelK AS modelK, " +
           "COUNT(s) AS memberCount, MAX(s.assignedAt) AS lastAssignedAt " +
           "FROM StudentClusterAssignment s " +
           "GROUP BY s.clusterId, s.clusterLabel, s.clusterTopErrors, s.clusterAvgAccuracy, s.modelK " +
           "ORDER BY s.clusterId ASC")
    List<AdminClusterGroupProjection> findClusterGroups();

    @Query("SELECT MAX(s.assignedAt) FROM StudentClusterAssignment s")
    LocalDateTime findLastRunAt();
}
