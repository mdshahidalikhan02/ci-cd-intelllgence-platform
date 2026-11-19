package com.platform.cicd.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Seeds a handful of sample runbooks into {@code incident_runbooks} on first startup so RAG similarity
 * search can be exercised immediately. Needs a real embedding provider (OPENAI_API_KEY); with the mock
 * key the failure is logged and startup continues.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.runbooks.seed-on-startup", havingValue = "true")
public class RunbookSeeder implements ApplicationRunner {

    private record Seed(String title, String content, String category) {
    }

    private static final List<Seed> SEEDS = List.of(
            new Seed("PostgreSQL connection slots exhausted",
                    "Symptom: PSQLException FATAL: remaining connection slots are reserved for roles with the "
                            + "SUPERUSER attribute, or HikariPool Connection is not available, request timed out. "
                            + "Cause: total pool sizes across services exceed max_connections. "
                            + "Fix: bound HikariCP maximum-pool-size per service, raise Postgres max_connections "
                            + "or add PgBouncer, and close leaked connections in integration tests.",
                    "DATABASE_CONNECTION_EXHAUSTION"),
            new Seed("Java heap space OutOfMemoryError in build or tests",
                    "Symptom: java.lang.OutOfMemoryError: Java heap space during mvn test or Gradle build. "
                            + "Cause: forked test JVM or build daemon has too little heap, or a test leaks memory. "
                            + "Fix: set -Xmx via MAVEN_OPTS or surefire argLine, reduce parallel forks, and "
                            + "capture a heap dump with -XX:+HeapDumpOnOutOfMemoryError.",
                    "OUT_OF_MEMORY"),
            new Seed("Maven dependency resolution failure",
                    "Symptom: Could not resolve dependencies, Could not transfer artifact, "
                            + "Non-resolvable parent POM, or 403/404 from the repository. "
                            + "Cause: artifact version missing from Maven Central, proxy or credential problem. "
                            + "Fix: pin an existing version, check repository mirrors and settings.xml "
                            + "credentials, and clear the corrupt ~/.m2 entry.",
                    "DEPENDENCY_RESOLUTION"),
            new Seed("Docker image pull rate limit or missing image",
                    "Symptom: toomanyrequests: You have reached your pull rate limit, or "
                            + "manifest unknown / pull access denied in CI. "
                            + "Fix: authenticate to the registry with docker login, use a pull-through cache "
                            + "or mirror, and pin image tags that exist.",
                    "CONTAINER_IMAGE_PULL"));

    private final RunbookRetrievalService retrievalService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer existing = jdbcTemplate.queryForObject("SELECT count(*) FROM incident_runbooks", Integer.class);
            if (existing != null && existing > 0) {
                log.info("incident_runbooks already contains {} rows; skipping seed", existing);
                return;
            }
            for (Seed seed : SEEDS) {
                retrievalService.ingestRunbook(seed.title(), seed.content(), Map.of("category", seed.category()));
            }
            log.info("Seeded {} sample runbooks", SEEDS.size());
        } catch (Exception e) {
            log.warn("Skipping runbook seeding (is a real OPENAI_API_KEY configured?): {}", e.getMessage());
        }
    }
}
