package com.platform.cicd.ingestion;

import com.platform.cicd.domain.OutboxEvent;
import com.platform.cicd.domain.OutboxStatus;
import com.platform.cicd.domain.WorkflowFailure;
import com.platform.cicd.domain.WorkflowStatus;
import com.platform.cicd.dto.FailureEventMessage;
import com.platform.cicd.dto.WebhookPayload;
import com.platform.cicd.repository.OutboxEventRepository;
import com.platform.cicd.repository.WorkflowFailureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WorkflowFailureRepository failureRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    @Transactional
    public WorkflowFailure ingestFailure(WebhookPayload payload) {
        log.debug("Persisting workflow failure and outbox event atomically for pipeline={}", payload.pipelineId());

        OffsetDateTime now = OffsetDateTime.now();

        WorkflowFailure failure = WorkflowFailure.builder()
                .repository(payload.repository())
                .branch(payload.branch())
                .commitSha(payload.commitSha())
                .pipelineId(payload.pipelineId())
                .rawLog(payload.rawLog())
                .status(WorkflowStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        WorkflowFailure savedFailure = failureRepository.save(failure);

        String eventJson = serializeEvent(new FailureEventMessage(
                savedFailure.getId(),
                savedFailure.getRepository(),
                savedFailure.getBranch(),
                savedFailure.getCommitSha(),
                savedFailure.getPipelineId()
        ));

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("WorkflowFailure")
                .aggregateId(savedFailure.getId().toString())
                .eventType("FAILURE_INGESTED")
                .payload(eventJson)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(now)
                .build();

        outboxEventRepository.save(outboxEvent);

        log.info("Committed WorkflowFailure id={} and OutboxEvent id={}",
                savedFailure.getId(), outboxEvent.getId());
        return savedFailure;
    }

    @Transactional(readOnly = true)
    public Optional<WorkflowFailure> findById(UUID id) {
        return failureRepository.findById(id);
    }

    private String serializeEvent(FailureEventMessage message) {
        try {
            return jsonMapper.writeValueAsString(message);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize FailureEventMessage", e);
        }
    }
}
