package com.example.spring.ai.strands.agent.execution.stream;

import com.example.spring.ai.strands.agent.execution.ToolCallRequest;

/**
 * Streaming events emitted during {@code StrandsExecutionLoop} streaming execution.
 *
 * @author Vaquar Khan
 */
public sealed interface StreamEvent permits StreamEvent.Token, StreamEvent.ToolCallBoundary, StreamEvent.Complete {

    record Token(String value) implements StreamEvent {
    }

    record ToolCallBoundary(ToolCallRequest request) implements StreamEvent {
    }

    record Complete(String finalText) implements StreamEvent {
    }
}
