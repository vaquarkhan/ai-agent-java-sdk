package io.github.vaquarkhan.strands.support;

/**
 * @author Vaquar Khan
 */
import io.github.vaquarkhan.strands.model.IterationRecord;
import io.github.vaquarkhan.strands.model.ReasoningTrace;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReasoningTraceBuilder {
    private String traceId = UUID.randomUUID().toString();
    private String agentId = "agent";
    private String sessionId = "session";
    private Instant startTime = Instant.now();
    private final List<IterationRecord> iterations = new ArrayList<>();

    public ReasoningTraceBuilder addIteration(IterationRecord record) {
        iterations.add(record);
        return this;
    }

    public ReasoningTrace build() {
        return new ReasoningTrace(traceId, agentId, sessionId, startTime, iterations);
    }
}
