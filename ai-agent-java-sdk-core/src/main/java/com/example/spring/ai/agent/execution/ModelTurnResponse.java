package com.example.spring.ai.agent.execution;

import java.util.List;

/**
 * Response from a single model turn, which may be a final answer, a single tool call,
 * or multiple parallel tool calls.
 *
 * @author Vaquar Khan
 */
public record ModelTurnResponse(String content, ToolCallRequest toolCallRequest, List<ToolCallRequest> toolCallRequests) {

    /**
     * Compact constructor that normalizes null tool call request lists.
     */
    public ModelTurnResponse {
        if (toolCallRequests == null) {
            toolCallRequests = List.of();
        }
    }

    public boolean hasToolCall() {
        return toolCallRequest != null || !toolCallRequests.isEmpty();
    }

    /**
     * Returns true when the model requested more than one tool call in a single turn.
     */
    public boolean hasMultipleToolCalls() {
        return toolCallRequests.size() > 1;
    }

    /**
     * Returns all tool call requests. If only a single tool call was provided,
     * it is wrapped in a singleton list for uniform handling.
     */
    public List<ToolCallRequest> allToolCallRequests() {
        if (!toolCallRequests.isEmpty()) {
            return toolCallRequests;
        }
        if (toolCallRequest != null) {
            return List.of(toolCallRequest);
        }
        return List.of();
    }

    public static ModelTurnResponse finalAnswer(String content) {
        return new ModelTurnResponse(content, null, List.of());
    }

    public static ModelTurnResponse toolCall(String toolName, String arguments) {
        return new ModelTurnResponse(null, new ToolCallRequest(toolName, arguments), List.of());
    }

    /**
     * Factory method for multiple parallel tool calls in a single model turn.
     *
     * @param requests the list of tool call requests
     * @return a response representing parallel tool calls
     */
    public static ModelTurnResponse multiToolCall(List<ToolCallRequest> requests) {
        return new ModelTurnResponse(null, null, List.copyOf(requests));
    }
}
