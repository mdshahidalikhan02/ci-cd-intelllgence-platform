package com.platform.cicd.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cicd.ai.DiagnosticEngine;
import com.platform.cicd.domain.WorkflowFailure;
import com.platform.cicd.domain.WorkflowStatus;
import com.platform.cicd.dto.FailureReport;
import com.platform.cicd.dto.GithubWebhookPayload;
import com.platform.cicd.parsing.LogExtractorService;
import com.platform.cicd.repository.WorkflowFailureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailureEventConsumer {

    private final WorkflowFailureRepository workflowFailureRepository;
    private final LogExtractorService logExtractorService;
    private final DiagnosticEngine diagnosticEngine;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.failure-events}", groupId = "cicd-intelligence-group")
    public void consumeFailureEvent(String payloadJson) {
        log.info("Kafka Consumer received event payload");

        try {
            GithubWebhookPayload payload = objectMapper.readValue(payloadJson, GithubWebhookPayload.class);
            String runId = payload.workflowRun().id();

            Optional<WorkflowFailure> failureOpt = workflowFailureRepository.findById(runId);
            if (failureOpt.isEmpty()) {
                log.warn("Workflow failure entity with ID {} not found in database. Skipping.", runId);
                return;
            }

            WorkflowFailure failure = failureOpt.get();
            if (failure.getStatus() == WorkflowStatus.ANALYZED) {
                log.info("Workflow run {} has already been analyzed. Idempotent skip.", runId);
                return;
            }

            failure.setStatus(WorkflowStatus.PROCESSING);
            workflowFailureRepository.save(failure);

            String simulatedLog = "ERROR: Connection to database failed on port 5432. " +
                    "java.sql.SQLException: Cannot create PoolableConnectionFactory (Connection refused)\n" +
                    "\tat org.postgresql.Driver.connect(Driver.java:298)\n" +
                    "BUILD FAILURE";

            String extractedStackTrace = logExtractorService.extractStackTrace(simulatedLog);
            failure.setParsedStacktrace(extractedStackTrace);

            FailureReport report = diagnosticEngine.analyzeFailure(extractedStackTrace);
            log.info("Analysis complete for run ID {}: Category={}, Confidence={}",
                    runId, report.failureCategory(), report.confidenceScore());

            failure.setRawLog(report.rootCauseSummary());
            failure.setStatus(WorkflowStatus.ANALYZED);
            workflowFailureRepository.save(failure);

        } catch (Exception e) {
            log.error("Failed to process event from Kafka listener", e);
        }
    }
}
