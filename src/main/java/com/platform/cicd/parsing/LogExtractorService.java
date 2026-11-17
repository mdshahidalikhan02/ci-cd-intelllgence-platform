package com.platform.cicd.parsing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LogExtractorService {

    private static final Pattern STACKTRACE_PATTERN = Pattern.compile(
            "(?s)(Exception|Error|FATAL|BUILD FAILURE|Caused by:).*?(?=\\n\\n|\\z)"
    );

    public String extractStackTrace(String rawLogs) {
        if (rawLogs == null || rawLogs.isBlank()) {
            return "No log output available for analysis.";
        }

        Matcher matcher = STACKTRACE_PATTERN.matcher(rawLogs);
        StringBuilder extracted = new StringBuilder();

        while (matcher.find()) {
            extracted.append(matcher.group().trim()).append("\n---\n");
        }

        if (extracted.isEmpty()) {
            int length = Math.min(rawLogs.length(), 2000);
            return rawLogs.substring(rawLogs.length() - length);
        }

        return extracted.toString();
    }
}
