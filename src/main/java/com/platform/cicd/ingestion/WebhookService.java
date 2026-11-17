package com.platform.cicd.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cicd.domain.OutboxEvent;
import com.platform.cicd.domain.OutboxStatus;
import com.platform.cicd.domain.WorkflowFailure;
import com.platform.cicd.domain.WorkflowStatus;
import com.platform.cicd.dto.GithubWebhookPayload;
import com.platform.cicd.repository.OutboxEventRepository;
import com.platform.cicd.repository.WorkflowFailureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WorkflowFailureRepository workflowFailureRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processGithubWebhook(GithubWebhookPayload payload) {
        if (payload.workflowRun() == null) {
            log.warn("Received webhook payload without workflow_run object. Action: {}", payload.action());
            return;
        }

        GithubWebhookPayload.WorkflowRun run = payload.workflowRun();

        if (!"completed".equalsIgnoreCase(run.status()) || !"failure".equalsIgnoreCase(run.conclusion())) {
            log.info("Ignoring workflow run ID: {}, status: {}, conclusion: {}", run.id(), run.status(), run.conclusion());
            return;
        }

        String repoName = run.repository() != null ? run.repository().fullName() : "unknown-repo";

        WorkflowFailure failure = WorkflowFailure.builder()
                .id(run.id())
                .repoName(repoName)
                .runAttempt(run.runAttempt())
                .rawLog("Log payload pending ingestion for run: " + run.id())
                .status(WorkflowStatus.RECEIVED)
                .build();

        workflowFailureRepository.save(failure);

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("WorkflowFailure")
                    .aggregateId(run.id())
                    .eventType("WORKFLOW_FAILURE_DETECTED")
                    .payload(payloadJson)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Successfully ingested workflow failure and saved outbox event for Run ID: {}", run.id());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook payload for Run ID: {}", run.id(), e);
            throw new RuntimeException("Serialization failure during webhook processing", e);
        }
    }
}
