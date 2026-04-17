package com.example.spring.ai.agent.model;

import java.time.Duration;

/**
 * @author Vaquar Khan
 */

public record AgentResponse(
        String content,
        ReasoningTrace reasoningTrace,
        TerminationReason terminationReason,
        int iterationCount,
        Duration totalDuration) {}
