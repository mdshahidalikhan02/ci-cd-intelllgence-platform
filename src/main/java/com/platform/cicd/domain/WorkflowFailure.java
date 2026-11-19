package com.platform.cicd.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_failures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "repository", nullable = false)
    private String repository;

    @Column(name = "branch", nullable = false)
    private String branch;

    @Column(name = "commit_sha", nullable = false, length = 100)
    private String commitSha;

    @Column(name = "pipeline_id", nullable = false, length = 100)
    private String pipelineId;

    @Column(name = "raw_log", nullable = false, columnDefinition = "TEXT")
    private String rawLog;

    @Column(name = "parsed_stacktrace", columnDefinition = "TEXT")
    private String parsedStacktrace;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WorkflowStatus status;

    @Column(name = "failure_category", length = 100)
    private String failureCategory;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "root_cause_summary", columnDefinition = "TEXT")
    private String rootCauseSummary;

    @Column(name = "remediation_steps", columnDefinition = "TEXT")
    private String remediationSteps;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.status == null) {
            this.status = WorkflowStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
