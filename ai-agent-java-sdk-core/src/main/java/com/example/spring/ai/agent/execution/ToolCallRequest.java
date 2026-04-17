package com.example.spring.ai.agent.execution;

/**
 * @author Vaquar Khan
 */

public record ToolCallRequest(String toolName, String arguments) {
}
