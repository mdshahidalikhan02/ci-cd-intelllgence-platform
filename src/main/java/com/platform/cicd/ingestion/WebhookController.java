package com.platform.cicd.ingestion;

import com.platform.cicd.domain.WorkflowFailure;
import com.platform.cicd.dto.WebhookPayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/webhooks/failures")
    public ResponseEntity<Map<String, Object>> receiveFailureWebhook(@Valid @RequestBody WebhookPayload payload) {
        log.info("Received CI/CD failure webhook for repository={}, pipelineId={}",
                payload.repository(), payload.pipelineId());

        WorkflowFailure saved = webhookService.ingestFailure(payload);

        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/failures/" + saved.getId()))
                .body(Map.of(
                "failureId", saved.getId(),
                "status", saved.getStatus(),
                "message", "Failure event queued for asynchronous AI diagnosis"
        ));
    }

    @GetMapping("/failures/{id}")
    public ResponseEntity<WorkflowFailure> getFailureDiagnosis(@PathVariable UUID id) {
        return webhookService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
