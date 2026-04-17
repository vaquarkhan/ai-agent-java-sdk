package com.example.spring.ai.agent.model;

import java.time.Duration;

/**
 * @author Vaquar Khan
 */

public record IterationRecord(
        int iterationNumber,
        String toolName,
        String toolInput,
        String toolOutput,
        IterationType type,
        Duration elapsed) {}
