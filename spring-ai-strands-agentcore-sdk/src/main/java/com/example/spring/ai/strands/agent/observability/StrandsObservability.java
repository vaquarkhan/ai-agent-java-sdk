package com.example.spring.ai.strands.agent.observability;

import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.model.IterationRecord;
import com.example.spring.ai.strands.agent.model.IterationType;
import com.example.spring.ai.strands.agent.model.ReasoningTrace;
import com.example.spring.ai.strands.agent.model.TerminationReason;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vaquar Khan
 */

public class StrandsObservability {

    private static final Logger logger = LoggerFactory.getLogger(StrandsObservability.class);
    private static final Pattern AWS_ACCESS_KEY_PATTERN = Pattern.compile("AKIA[0-9A-Z]{16}");
    private static final Pattern JWT_PATTERN = Pattern.compile("[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");

    private final MeterRegistry meterRegistry;
    private final ObservationRegistry observationRegistry;
    private final StrandsAgentProperties.Security security;

    public StrandsObservability(
            MeterRegistry meterRegistry,
            ObservationRegistry observationRegistry,
            StrandsAgentProperties.Security security) {
        this.meterRegistry = meterRegistry;
        this.observationRegistry = observationRegistry == null ? ObservationRegistry.NOOP : observationRegistry;
        this.security = security;
    }

    public ReasoningTrace startTrace(String agentId, StrandsExecutionContext context) {
        return new ReasoningTrace(UUID.randomUUID().toString(), agentId, context.getSessionId(), Instant.now(), List.of());
    }

    public ReasoningTrace recordIteration(
            ReasoningTrace trace,
            int iterationNumber,
            String toolName,
            String toolInput,
            String toolOutput,
            Duration elapsed) {
        Observation iterationObservation = Observation.start("strands.iteration", observationRegistry)
                .lowCardinalityKeyValue("tool.name", nullToEmpty(toolName))
                .lowCardinalityKeyValue("iteration", String.valueOf(iterationNumber));
        try (Observation.Scope ignored = iterationObservation.openScope()) {
            String traceOutput = truncateAndSanitize(toolOutput);
            IterationType iterationType = toolName == null ? IterationType.FINAL_ANSWER : IterationType.TOOL_CALL;
            IterationRecord record =
                    new IterationRecord(iterationNumber, toolName, maybeTraceData(toolInput), traceOutput, iterationType, elapsed);
            List<IterationRecord> updated = new ArrayList<>(trace.iterations());
            updated.add(record);
            logger.debug("strands iteration={} tool={} summary={}", iterationNumber, toolName, iterationType);
            logger.info("strands iteration={} tool={} elapsedMs={}",
                    iterationNumber, toolName, elapsed.toMillis());
            return new ReasoningTrace(trace.traceId(), trace.agentId(), trace.sessionId(), trace.startTime(), updated);
        } finally {
            iterationObservation.stop();
        }
    }

    public ReasoningTrace recordCompletion(
            ReasoningTrace trace, TerminationReason reason, int totalIterations, Duration totalElapsed) {
        recordMetrics(trace, reason, totalIterations, totalElapsed);
        return trace;
    }

    public void recordMetrics(
            ReasoningTrace trace, TerminationReason reason, int totalIterations, Duration totalElapsed) {
        DistributionSummary.builder("strands.iteration.count").register(meterRegistry).record(totalIterations);
        Timer.builder("strands.loop.duration").register(meterRegistry).record(totalElapsed);
        if (reason == TerminationReason.MAX_ITERATIONS_REACHED) {
            Counter.builder("strands.loop.max_iteration_termination").register(meterRegistry).increment();
        }
        trace.iterations().stream()
                .filter(record -> record.toolName() != null)
                .forEach(record -> Counter.builder("strands.tool.invocations")
                        .tag("tool", record.toolName())
                        .register(meterRegistry)
                        .increment());
    }

    private String truncateAndSanitize(String value) {
        if (value == null) {
            return null;
        }
        String output = value;
        if (security.isSanitizeToolOutput()) {
            output = sanitize(output);
        }
        int maxLen = security.getTraceMaxOutputLength();
        if (output.length() > maxLen) {
            return output.substring(0, maxLen);
        }
        return output;
    }

    public String sanitize(String value) {
        String sanitized = AWS_ACCESS_KEY_PATTERN.matcher(value).replaceAll("[REDACTED_AWS_KEY]");
        sanitized = JWT_PATTERN.matcher(sanitized).replaceAll("[REDACTED_JWT]");
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("[REDACTED_EMAIL]");
        return sanitized;
    }

    private String maybeTraceData(String value) {
        if (security.isTraceIncludeToolData()) {
            return truncateAndSanitize(value);
        }
        return null;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
