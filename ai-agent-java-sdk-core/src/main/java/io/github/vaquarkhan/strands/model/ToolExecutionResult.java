package io.github.vaquarkhan.strands.model;

import java.time.Duration;

/**
 * @author Vaquar Khan
 */

public record ToolExecutionResult(
        String toolName, boolean success, String output, Duration executionTime) {}
