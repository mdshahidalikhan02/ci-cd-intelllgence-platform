package com.platform.cicd.dto;

import java.util.List;

public record FailureReport(
    String failureCategory,
    double confidenceScore,
    String rootCauseSummary,
    List<String> evidenceLines,
    List<String> recommendedFixes
) {}
