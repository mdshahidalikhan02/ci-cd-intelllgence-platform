package com.platform.cicd.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubWebhookPayload(
    String action,
    @JsonProperty("workflow_run") WorkflowRun workflowRun
) {
    public record WorkflowRun(
        String id,
        String name,
        String status,
        String conclusion,
        @JsonProperty("run_attempt") int runAttempt,
        @JsonProperty("html_url") String htmlUrl,
        Repository repository,
        @JsonProperty("head_commit") HeadCommit headCommit
    ) {}

    public record Repository(
        @JsonProperty("full_name") String fullName
    ) {}

    public record HeadCommit(
        String id,
        String message
    ) {}
}
