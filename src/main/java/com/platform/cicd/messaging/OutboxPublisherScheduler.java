package com.platform.cicd.messaging;

import com.platform.cicd.domain.OutboxEvent;
import com.platform.cicd.domain.OutboxStatus;
import com.platform.cicd.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisherScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topics.failure-events}")
    private String failureEventsTopic;

    @Value("${app.outbox.max-retries:3}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPendingOutboxEvents() {
        List<OutboxEvent> pendingEvents =
                outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to relay", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(failureEventsTopic, event.getAggregateId(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);

                event.setStatus(OutboxStatus.PROCESSED);
                event.setProcessedAt(OffsetDateTime.now());
                log.info("Published OutboxEvent id={} to topic={}", event.getId(), failureEventsTopic);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Outbox relay interrupted; remaining events stay PENDING");
                outboxEventRepository.save(event);
                return;
            } catch (Exception ex) {
                int updatedRetries = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
                event.setRetryCount(updatedRetries);
                log.error("Failed to publish OutboxEvent id={} (attempt {}/{})",
                        event.getId(), updatedRetries, maxRetries, ex);

                if (updatedRetries >= maxRetries) {
                    event.setStatus(OutboxStatus.FAILED);
                    log.error("OutboxEvent id={} exceeded max retries and moved to FAILED", event.getId());
                }
            }
            outboxEventRepository.save(event);
        }
    }
}
