package com.example.spring.ai.agent.execution;

import com.example.spring.ai.agent.model.ReasoningTrace;
import com.example.spring.ai.agent.model.TerminationReason;
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
