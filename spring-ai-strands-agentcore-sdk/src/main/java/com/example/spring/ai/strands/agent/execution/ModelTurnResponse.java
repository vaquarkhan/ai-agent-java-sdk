package com.example.spring.ai.strands.agent.execution;

/**
 * @author Vaquar Khan
 */

public record ModelTurnResponse(String content, ToolCallRequest toolCallRequest) {

    public boolean hasToolCall() {
        return toolCallRequest != null;
    }

    public static ModelTurnResponse finalAnswer(String content) {
        return new ModelTurnResponse(content, null);
    }

    public static ModelTurnResponse toolCall(String toolName, String arguments) {
        return new ModelTurnResponse(null, new ToolCallRequest(toolName, arguments));
    }
}
