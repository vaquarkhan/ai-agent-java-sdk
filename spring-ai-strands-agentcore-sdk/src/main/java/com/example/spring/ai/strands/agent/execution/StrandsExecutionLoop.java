package com.example.spring.ai.strands.agent.execution;

import com.example.spring.ai.strands.agent.execution.stream.StreamEvent;
import com.example.spring.ai.strands.agent.model.ReasoningTrace;
import com.example.spring.ai.strands.agent.model.TerminationReason;
import com.example.spring.ai.strands.agent.model.ToolExecutionResult;
import com.example.spring.ai.strands.agent.observability.StrandsObservability;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * @author Vaquar Khan
 */

public class StrandsExecutionLoop {

    private final LoopModelClient modelClient;
    private final int maxIterations;
    private final String agentId;

    public StrandsExecutionLoop(LoopModelClient modelClient, int maxIterations, String agentId) {
        this.modelClient = modelClient;
        this.maxIterations = maxIterations;
        this.agentId = agentId;
    }

    public StrandsLoopResult run(
            String systemPrompt,
            List<ExecutionMessage> messages,
            ToolRegistry toolRegistry,
            StrandsObservability observability,
            StrandsExecutionContext context) {
        List<ExecutionMessage> history = new ArrayList<>(messages);
        toolRegistry.beginNewLoop();
        ReasoningTrace trace = observability.startTrace(agentId, context);
        Instant loopStart = Instant.now();
        String partialContent = "";
        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            Instant iterationStart = Instant.now();
            try {
                ModelTurnResponse response = modelClient.generate(withSystemPrompt(systemPrompt, history),
                        toolRegistry.getToolCallbacks());
                if (response.hasToolCall()) {
                    ToolCallRequest request = response.toolCallRequest();
                    ToolExecutionResult toolResult = toolRegistry.executeTool(request.toolName(), request.arguments());
                    String toolOutput = toolResult.output();
                    trace = observability.recordIteration(trace, iteration, request.toolName(), request.arguments(),
                            toolOutput, Duration.between(iterationStart, Instant.now()));
                    history.add(new ExecutionMessage("tool", toolOutput));
                    continue;
                }
                partialContent = response.content();
                trace = observability.recordIteration(trace, iteration, null, null, response.content(),
                        Duration.between(iterationStart, Instant.now()));
                Duration elapsed = Duration.between(loopStart, Instant.now());
                trace = observability.recordCompletion(trace, TerminationReason.MODEL_COMPLETION, iteration, elapsed);
                return new StrandsLoopResult(partialContent, trace, TerminationReason.MODEL_COMPLETION, iteration, elapsed);
            } catch (Exception exception) {
                Duration elapsed = Duration.between(loopStart, Instant.now());
                observability.recordCompletion(trace, TerminationReason.ERROR, iteration - 1, elapsed);
                throw new StrandsExecutionException(
                        "Execution failed at iteration " + iteration, exception, iteration, trace);
            }
        }
        Duration elapsed = Duration.between(loopStart, Instant.now());
        trace = observability.recordCompletion(trace, TerminationReason.MAX_ITERATIONS_REACHED, maxIterations, elapsed);
        return new StrandsLoopResult(partialContent, trace, TerminationReason.MAX_ITERATIONS_REACHED, maxIterations, elapsed);
    }

    public Flux<String> runStreaming(
            String systemPrompt,
            List<ExecutionMessage> messages,
            ToolRegistry toolRegistry,
            StrandsObservability observability,
            StrandsExecutionContext context) {
        return Flux.<String>create(sink -> streamLoop(systemPrompt, messages, toolRegistry, observability, context, sink))
                .onBackpressureBuffer(256, ignored -> {
                }, BufferOverflowStrategy.ERROR);
    }

    private void streamLoop(
            String systemPrompt,
            List<ExecutionMessage> messages,
            ToolRegistry toolRegistry,
            StrandsObservability observability,
            StrandsExecutionContext context,
            FluxSink<String> sink) {
        List<ExecutionMessage> history = new ArrayList<>(messages);
        toolRegistry.beginNewLoop();
        ReasoningTrace trace = observability.startTrace(agentId, context);
        AtomicBoolean completed = new AtomicBoolean(false);
        try {
            for (int iteration = 1; iteration <= maxIterations && !sink.isCancelled(); iteration++) {
                Instant iterationStart = Instant.now();
                List<StreamEvent> events = modelClient.stream(withSystemPrompt(systemPrompt, history),
                                toolRegistry.getToolCallbacks())
                        .collectList()
                        .block();
                if (events == null) {
                    sink.error(new IllegalStateException("No streaming events received"));
                    return;
                }
                StringBuilder emitted = new StringBuilder();
                boolean hadToolCall = false;
                for (StreamEvent event : events) {
                    if (event instanceof StreamEvent.Token token) {
                        emitted.append(token.value());
                        sink.next(token.value());
                    } else if (event instanceof StreamEvent.ToolCallBoundary boundary) {
                        hadToolCall = true;
                        ToolExecutionResult toolResult = toolRegistry.executeTool(
                                boundary.request().toolName(), boundary.request().arguments());
                        trace = observability.recordIteration(trace, iteration, boundary.request().toolName(),
                                boundary.request().arguments(), toolResult.output(),
                                Duration.between(iterationStart, Instant.now()));
                        history.add(new ExecutionMessage("tool", toolResult.output()));
                    } else if (event instanceof StreamEvent.Complete complete) {
                        if (!complete.finalText().isBlank()) {
                            history.add(new ExecutionMessage("assistant", complete.finalText()));
                        }
                        if (!hadToolCall) {
                            trace = observability.recordIteration(trace, iteration, null, null, complete.finalText(),
                                    Duration.between(iterationStart, Instant.now()));
                            observability.recordCompletion(trace, TerminationReason.MODEL_COMPLETION, iteration,
                                    Duration.between(trace.startTime(), Instant.now()));
                            completed.set(true);
                            sink.complete();
                            return;
                        }
                    }
                }
                if (!hadToolCall && emitted.length() > 0) {
                    trace = observability.recordIteration(trace, iteration, null, null, emitted.toString(),
                            Duration.between(iterationStart, Instant.now()));
                    observability.recordCompletion(trace, TerminationReason.MODEL_COMPLETION, iteration,
                            Duration.between(trace.startTime(), Instant.now()));
                    completed.set(true);
                    sink.complete();
                    return;
                }
            }
            if (!completed.get() && !sink.isCancelled()) {
                sink.error(new IllegalStateException("Max iterations reached during streaming execution"));
            }
        } catch (Exception exception) {
            sink.error(new StrandsExecutionException("Streaming execution failed", exception, 0, trace));
        }
    }

    private List<ExecutionMessage> withSystemPrompt(String systemPrompt, List<ExecutionMessage> history) {
        List<ExecutionMessage> merged = new ArrayList<>();
        merged.add(new ExecutionMessage("system", systemPrompt));
        merged.addAll(history);
        return merged;
    }
}
