package io.github.vaquarkhan.agent.execution;

import io.github.vaquarkhan.agent.model.ReasoningTrace;
import io.github.vaquarkhan.agent.model.TerminationReason;
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
