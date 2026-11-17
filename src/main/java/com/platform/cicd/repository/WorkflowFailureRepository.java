package com.platform.cicd.repository;

import com.platform.cicd.domain.WorkflowFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowFailureRepository extends JpaRepository<WorkflowFailure, String> {
}
