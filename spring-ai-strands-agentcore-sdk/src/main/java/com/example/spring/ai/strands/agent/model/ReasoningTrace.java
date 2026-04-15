package com.example.spring.ai.strands.agent.model;

import java.time.Instant;
import java.util.List;

/**
 * @author Vaquar Khan
 */

public record ReasoningTrace(
        String traceId,
        String agentId,
        String sessionId,
        Instant startTime,
        List<IterationRecord> iterations) {
    public ReasoningTrace {
        iterations = iterations == null ? List.of() : List.copyOf(iterations);
    }
}
