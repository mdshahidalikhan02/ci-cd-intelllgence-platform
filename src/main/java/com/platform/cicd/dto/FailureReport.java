package com.platform.cicd.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record FailureReport(
        @JsonPropertyDescription("Short UPPER_SNAKE_CASE failure category, e.g. DATABASE_CONNECTION_EXHAUSTION")
        String failureCategory,
        @JsonPropertyDescription("Confidence in the diagnosis as a number between 0.0 and 1.0")
        double confidenceScore,
        @JsonPropertyDescription("One or two sentence root-cause summary")
        String rootCauseSummary,
        @JsonPropertyDescription("Verbatim log lines that support the diagnosis")
        List<String> keyEvidence,
        @JsonPropertyDescription("Ordered, actionable remediation steps")
        List<String> remediationSteps
) {
}
