package io.github.vaquarkhan.strands.execution;

import io.github.vaquarkhan.strands.model.ReasoningTrace;
import io.github.vaquarkhan.strands.model.TerminationReason;
import java.time.Duration;

/**
 * @author Vaquar Khan
 */

public record AgentLoopResult(
        String content,
        ReasoningTrace reasoningTrace,
        TerminationReason terminationReason,
        int iterationCount,
        Duration totalDuration
) {
}
