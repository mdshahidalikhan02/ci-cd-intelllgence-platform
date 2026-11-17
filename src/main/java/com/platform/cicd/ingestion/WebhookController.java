package com.platform.cicd.ingestion;

import com.platform.cicd.dto.GithubWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/github")
    public ResponseEntity<String> handleGithubWebhook(@RequestBody GithubWebhookPayload payload) {
        log.info("Received GitHub Webhook request for action: {}", payload.action());
        webhookService.processGithubWebhook(payload);
        return ResponseEntity.ok("Webhook received successfully");
    }
}
