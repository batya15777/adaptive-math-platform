package com.adaptive.server.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Persisted result of the last K-Means run for a student: which behavioural
 * cluster they belong to. One row per user (upserted on every run) so the
 * dynamic question generator can later read a student's cluster with a single
 * {@code findByUserId} lookup.
 */
//לשמור לאיזו קבוצה (Cluster) שייך כל תלמיד, כדי שמחולל השאלות הדינמי
//יוכל לשלוף את המידע הזה בשבריר שנייה בלי להריץ את אלגוריתם הלמידה מחדש
@Entity
@Table(name = "student_cluster_assignments",
        uniqueConstraints = @UniqueConstraint(name = "uq_cluster_user", columnNames = "user_id"))
public class StudentClusterAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stored as a plain id (not a @ManyToOne) to keep this mapping table decoupled
    // and cheap to upsert from the batch clustering job.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cluster_id", nullable = false)
    private Integer clusterId;

    @Column(name = "cluster_label")
    private String clusterLabel;

    // The cohort's dominant error patterns (comma-joined, from the ML summary).
    // Drives adaptive recommendations and the cluster-aware question generators.
    @Column(name = "cluster_top_errors", length = 512)
    private String clusterTopErrors;

    // The cohort's average accuracy [0..1] — used to bias question difficulty.
    @Column(name = "cluster_avg_accuracy")
    private Double clusterAvgAccuracy;

    // k used by the model run that produced this assignment (for traceability).
    @Column(name = "model_k")
    private Integer modelK;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    public StudentClusterAssignment() {
    }

    public StudentClusterAssignment(Long userId) {
        this.userId = userId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getClusterId() { return clusterId; }
    public void setClusterId(Integer clusterId) { this.clusterId = clusterId; }

    public String getClusterLabel() { return clusterLabel; }
    public void setClusterLabel(String clusterLabel) { this.clusterLabel = clusterLabel; }

    public String getClusterTopErrors() { return clusterTopErrors; }
    public void setClusterTopErrors(String clusterTopErrors) { this.clusterTopErrors = clusterTopErrors; }

    public Double getClusterAvgAccuracy() { return clusterAvgAccuracy; }
    public void setClusterAvgAccuracy(Double clusterAvgAccuracy) { this.clusterAvgAccuracy = clusterAvgAccuracy; }

    public Integer getModelK() { return modelK; }
    public void setModelK(Integer modelK) { this.modelK = modelK; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
}
