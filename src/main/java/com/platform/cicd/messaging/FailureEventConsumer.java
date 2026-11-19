package com.platform.cicd.messaging;

import com.platform.cicd.ai.DiagnosticEngine;
import com.platform.cicd.domain.WorkflowFailure;
import com.platform.cicd.domain.WorkflowStatus;
import com.platform.cicd.dto.FailureEventMessage;
import com.platform.cicd.dto.FailureReport;
import com.platform.cicd.repository.WorkflowFailureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Deliberately NOT {@code @Transactional}: the LLM call can take many seconds and must not pin a DB
 * connection/transaction. Each repository call commits on its own, so the PROCESSING state is visible
 * and a crash mid-flight leaves a recoverable row (redelivery re-processes anything that is not ANALYZED).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FailureEventConsumer {

    static final int MAX_LOG_LINES = 60;
    static final int MAX_LOG_CHARS = 6_000;

    private final WorkflowFailureRepository failureRepository;
    private final DiagnosticEngine diagnosticEngine;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "${app.kafka.topics.failure-events}")
    public void handleFailureEvent(String rawMessage) {
        FailureEventMessage event;
        try {
            event = jsonMapper.readValue(rawMessage, FailureEventMessage.class);
        } catch (Exception e) {
            log.error("Discarding malformed Kafka payload: {}", rawMessage, e);
            return;
        }
        if (event == null || event.failureId() == null) {
            log.error("Discarding Kafka payload without failureId: {}", rawMessage);
            return;
        }

        log.info("Consumed failure event for failureId={}", event.failureId());

        WorkflowFailure failure = failureRepository.findById(event.failureId()).orElse(null);
        if (failure == null) {
            log.warn("WorkflowFailure not found for id={}", event.failureId());
            return;
        }

        // Idempotency: at-least-once delivery means duplicates are expected.
        if (failure.getStatus() == WorkflowStatus.ANALYZED) {
            log.info("Skipping duplicate event for already analyzed failureId={}", failure.getId());
            return;
        }

        failure.setStatus(WorkflowStatus.PROCESSING);
        failure = failureRepository.save(failure);

        try {
            String extractedStacktrace = extractRelevantLogSlice(failure.getRawLog());
            failure.setParsedStacktrace(extractedStacktrace);

            FailureReport report = diagnosticEngine.analyzeFailure(extractedStacktrace);

            failure.setFailureCategory(report.failureCategory());
            failure.setConfidenceScore(Math.max(0.0, Math.min(1.0, report.confidenceScore())));
            failure.setRootCauseSummary(report.rootCauseSummary());
            failure.setRemediationSteps(
                    report.remediationSteps() == null ? "" : String.join("\n", report.remediationSteps()));
            failure.setStatus(WorkflowStatus.ANALYZED);
            failureRepository.save(failure);

            log.info("Completed AI root-cause diagnosis for failureId={} category={}",
                    failure.getId(), report.failureCategory());
        } catch (Exception e) {
            log.error("Diagnostic processing failed for failureId={}", failure.getId(), e);
            failure.setStatus(WorkflowStatus.FAILED);
            failureRepository.save(failure);
        }
    }

    /** Last {@value #MAX_LOG_LINES} lines of the log, capped at {@value #MAX_LOG_CHARS} characters (tail kept). */
    static String extractRelevantLogSlice(String rawLog) {
        if (rawLog == null || rawLog.isBlank()) {
            return "";
        }
        String[] lines = rawLog.split("\\R");
        int start = Math.max(0, lines.length - MAX_LOG_LINES);
        String slice = Arrays.stream(lines, start, lines.length).collect(Collectors.joining("\n"));
        return slice.length() > MAX_LOG_CHARS ? slice.substring(slice.length() - MAX_LOG_CHARS) : slice;
    }
}
