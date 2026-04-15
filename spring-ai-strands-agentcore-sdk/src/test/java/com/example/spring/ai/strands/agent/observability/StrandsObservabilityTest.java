package com.example.spring.ai.strands.agent.observability;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.model.TerminationReason;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrandsObservabilityTest {

    @Test
    void tracesAndMetricsRecorded() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        StrandsAgentProperties.Security security = new StrandsAgentProperties.Security();
        StrandsObservability observability = new StrandsObservability(meterRegistry, ObservationRegistry.NOOP, security);
        var trace = observability.startTrace("agent", StrandsExecutionContext.standalone("s1"));
        trace = observability.recordIteration(trace, 1, "calc", "{}", "4", Duration.ofMillis(5));
        trace = observability.recordCompletion(trace, TerminationReason.MODEL_COMPLETION, 1, Duration.ofMillis(10));
        observability.recordMetrics(trace, TerminationReason.MODEL_COMPLETION, 1, Duration.ofMillis(10));

        assertEquals("agent", trace.agentId());
        assertEquals("s1", trace.sessionId());
        assertEquals(1, trace.iterations().size());
        assertTrue(meterRegistry.getMeters().size() > 0);
    }

    @Test
    void sanitizerAndTruncationApplied() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        StrandsAgentProperties.Security security = new StrandsAgentProperties.Security();
        security.setSanitizeToolOutput(true);
        security.setTraceMaxOutputLength(10);
        StrandsObservability observability = new StrandsObservability(meterRegistry, ObservationRegistry.NOOP, security);
        String sanitized = observability.sanitize("AKIA1234567890123456 test@example.com abc.def.ghi");
        assertTrue(sanitized.contains("REDACTED"));
        var trace = observability.startTrace("agent", StrandsExecutionContext.standalone("s1"));
        trace = observability.recordIteration(trace, 1, "tool", "in", "0123456789abcdef", Duration.ofMillis(1));
        assertEquals(10, trace.iterations().get(0).toolOutput().length());
    }
}
