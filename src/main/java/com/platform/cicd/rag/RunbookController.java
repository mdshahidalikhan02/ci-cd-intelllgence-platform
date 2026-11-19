package com.platform.cicd.rag;

import com.platform.cicd.dto.RunbookEntryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/runbooks")
@RequiredArgsConstructor
public class RunbookController {

    private final RunbookRetrievalService retrievalService;

    @PostMapping
    public ResponseEntity<Map<String, String>> addRunbook(@Valid @RequestBody RunbookEntryRequest request) {
        retrievalService.ingestRunbook(request.title(), request.content(), request.metadata());
        return ResponseEntity.ok(Map.of(
                "status", "INDEXED",
                "title", request.title()
        ));
    }
}
