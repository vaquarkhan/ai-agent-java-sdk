package com.example.spring.ai.strands.agent.execution;

import com.example.spring.ai.strands.agent.model.ReasoningTrace;
import com.example.spring.ai.strands.agent.model.TerminationReason;
import java.time.Duration;

/**
 * @author Vaquar Khan
 */

public record StrandsLoopResult(
        String content,
        ReasoningTrace reasoningTrace,
        TerminationReason terminationReason,
        int iterationCount,
        Duration totalDuration
) {
}
