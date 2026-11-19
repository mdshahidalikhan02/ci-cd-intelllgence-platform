package com.platform.cicd.rag;

import com.platform.cicd.config.RunbookIndexingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    SearchRequest.builder()
                            .query(stackTrace)
                            .topK(3)
                            .similarityThreshold(0.65)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to perform vector similarity search", e);
            return Collections.emptyList();
        }
    }

    public void ingestRunbook(String title, String content, Map<String, Object> metadata) {
        Map<String, Object> enrichedMetadata = new HashMap<>();
        if (metadata != null) {
            enrichedMetadata.putAll(metadata);
        }
        enrichedMetadata.put("title", title);

        Document document = Document.builder()
                .text(title + "\n\n" + content)
                .metadata(enrichedMetadata)
                .build();
        try {
            vectorStore.add(List.of(document));
        } catch (Exception e) {
            throw new RunbookIndexingException("could not index runbook '" + title + "'", e);
        }
        log.info("Indexed runbook document title='{}' into pgvector store", title);
    }
}
