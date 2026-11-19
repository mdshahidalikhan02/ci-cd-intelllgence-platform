package com.platform.cicd.repository;

import com.platform.cicd.domain.WorkflowFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkflowFailureRepository extends JpaRepository<WorkflowFailure, UUID> {
}
