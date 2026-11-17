package com.platform.cicd.ai;

import com.platform.cicd.dto.FailureReport;
import com.platform.cicd.rag.RunbookRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosticEngine {

    private final ChatModel chatModel;
    private final RunbookRetrievalService retrievalService;

    public FailureReport analyzeFailure(String stackTrace) {
        List<Document> relevantDocs = retrievalService.findMatchingRunbooks(stackTrace);
        
        String context = relevantDocs.isEmpty() ? "No prior runbook matches found." :
                relevantDocs.stream()
                        .map(Document::getContent)
                        .collect(Collectors.joining("\n---\n"));

        BeanOutputConverter<FailureReport> parser = new BeanOutputConverter<>(FailureReport.class);

        String promptString = """
                You are a Senior Site Reliability Engineer (SRE).
                Analyze the following CI/CD failure log and contextual historical runbooks to determine root cause.
                
                LOG EXCERPT:
                {log}
                RELEVANT RUNBOOKS & HISTORICAL INCIDENTS:
                {context}
                
                {format}
                """;

        PromptTemplate template = new PromptTemplate(promptString);
        Prompt prompt = template.create(Map.of(
                "log", stackTrace,
                "context", context,
                "format", parser.getFormat()
        ));

        try {
            String rawResponse = chatModel.call(prompt).getResult().getOutput().getContent();
            return parser.convert(rawResponse);
        } catch (Exception e) {
            log.error("Error invoking LLM or parsing output schema", e);
            return new FailureReport(
                    "UNKNOWN_FAILURE",
                    0.0,
                    "Failed to process diagnosis due to model exception: " + e.getMessage(),
                    List.of(stackTrace),
                    List.of("Check raw build logs manually.")
            );
        }
    }
}
