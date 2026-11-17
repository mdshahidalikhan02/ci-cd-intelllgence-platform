package com.platform.cicd.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunbookRetrievalService {

    private final VectorStore vectorStore;

    public List<Document> findMatchingRunbooks(String stackTrace) {
        if (stackTrace == null || stackTrace.isBlank()) {
            return Collections.emptyList();
        }

        try {
            log.info("Executing vector similarity search against runbook knowledge base");
            return vectorStore.similaritySearch(
                    SearchRequest.query(stackTrace)
                            .withTopK(3)
                            .withSimilarityThreshold(0.65)
            );
        } catch (Exception e) {
            log.error("Failed to perform vector similarity search", e);
            return Collections.emptyList();
        }
    }
}
