package com.platform.cicd.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "workflow_failures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowFailure {

    @Id
    private String id;

    @Column(name = "repo_name", nullable = false)
    private String repoName;

    @Column(name = "run_attempt", nullable = false)
    private Integer runAttempt;

    @Column(name = "raw_log", columnDefinition = "TEXT")
    private String rawLog;

    @Column(name = "parsed_stacktrace", columnDefinition = "TEXT")
    private String parsedStacktrace;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkflowStatus status;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onPrePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
