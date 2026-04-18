package io.github.vaquarkhan.agent.execution.stream;

import io.github.vaquarkhan.agent.execution.ToolCallRequest;

/**
 * Streaming events emitted during {@code AgentExecutionLoop} streaming execution.
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
