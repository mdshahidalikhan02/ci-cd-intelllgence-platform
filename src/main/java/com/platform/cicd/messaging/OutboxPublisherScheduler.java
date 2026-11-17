package com.platform.cicd.messaging;

import com.platform.cicd.domain.OutboxEvent;
import com.platform.cicd.domain.OutboxStatus;
import com.platform.cicd.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisherScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topics.failure-events}")
    private String topicName;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void publishPendingOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, 50)
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending outbox events for publishing to Kafka", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(topicName, event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.info("Successfully published outbox event {} to topic {}", event.getId(), topicName);
                            } else {
                                log.error("Failed to publish outbox event {}", event.getId(), ex);
                            }
                        });

                event.setStatus(OutboxStatus.PROCESSED);
                event.setProcessedAt(OffsetDateTime.now());
            } catch (Exception e) {
                log.error("Exception handling outbox event publishing for ID: {}", event.getId(), e);
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 3) {
                    event.setStatus(OutboxStatus.FAILED);
                }
            }
        }
        outboxEventRepository.saveAll(pendingEvents);
    }
}
