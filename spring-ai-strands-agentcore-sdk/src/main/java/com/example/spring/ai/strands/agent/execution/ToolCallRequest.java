package com.example.spring.ai.strands.agent.execution;

/**
 * @author Vaquar Khan
 */

public record ToolCallRequest(String toolName, String arguments) {
}
