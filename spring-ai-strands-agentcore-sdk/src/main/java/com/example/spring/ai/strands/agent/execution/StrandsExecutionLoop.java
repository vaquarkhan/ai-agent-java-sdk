package com.example.spring.ai.strands.agent.execution;

import com.example.spring.ai.strands.agent.conversation.ConversationManager;
import com.example.spring.ai.strands.agent.execution.stream.StreamEvent;
import com.example.spring.ai.strands.agent.hook.HookRegistry;
import com.example.spring.ai.strands.agent.hook.StrandsHookEvent;
import com.example.spring.ai.strands.agent.hook.ToolCallPolicyDecision;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * Core execution loop that drives the model-tool interaction cycle.
 *
 * <p>Supports hook dispatch, conversation management, parallel tool execution,
 * and configurable retry on model errors.
 *
 * @author Vaquar Khan
 */
public class StrandsExecutionLoop {

    private static final Logger log = LoggerFactory.getLogger(StrandsExecutionLoop.class);

    private final LoopModelClient modelClient;
    private final int maxIterations;
    private final String agentId;
    private HookRegistry hookRegistry;
    private ConversationManager conversationManager;
    private boolean retryEnabled;
    private int maxRetries;
    private int backoffMillis;

    public StrandsExecutionLoop(LoopModelClient modelClient, int maxIterations, String agentId) {
        this.modelClient = modelClient;
        this.maxIterations = maxIterations;
        this.agentId = agentId;
    }

    public void setHookRegistry(HookRegistry hookRegistry) {
        this.hookRegistry = hookRegistry;
    }

    public HookRegistry getHookRegistry() {
        return hookRegistry;
    }

    public void setConversationManager(ConversationManager conversationManager) {
        this.conversationManager = conversationManager;
    }

    public void setRetryEnabled(boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setBackoffMillis(int backoffMillis) {
        this.backoffMillis = backoffMillis;
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
                // Apply conversation manager before building the prompt
                List<ExecutionMessage> managed = applyConversationManager(history, context);
                List<ExecutionMessage> prompt = withSystemPrompt(systemPrompt, managed);

                // Dispatch before-model-call hook
                dispatchHook(new StrandsHookEvent.BeforeModelCall(iteration, prompt));

                ModelTurnResponse response = callModelWithRetry(prompt, toolRegistry);

                // Dispatch after-model-call hook
                dispatchHook(new StrandsHookEvent.AfterModelCall(iteration, response));

                // Handle parallel tool calls
                if (response.hasMultipleToolCalls()) {
                    List<ToolCallRequest> requests = response.toolCallRequests();
                    List<ToolCallRequest> allowedRequests = new ArrayList<>();
                    for (ToolCallRequest request : requests) {
                        ToolCallEvaluation eval = evaluateToolCall(iteration, request);
                        if (!eval.allowed()) {
                            ToolExecutionResult deniedResult = new ToolExecutionResult(
                                    request.toolName(), false, eval.denialOutput(), Duration.ZERO);
                            dispatchHook(new StrandsHookEvent.AfterToolCall(iteration, request.toolName(), deniedResult));
                            trace = observability.recordIteration(trace, iteration, request.toolName(), request.arguments(),
                                    eval.denialOutput(), Duration.between(iterationStart, Instant.now()));
                            history.add(new ExecutionMessage("tool", eval.denialOutput()));
                            continue;
                        }
                        ToolCallRequest controlled = eval.request();
                        dispatchHook(new StrandsHookEvent.BeforeToolCall(
                                iteration, controlled.toolName(), controlled.arguments()));
                        allowedRequests.add(controlled);
                    }
                    if (allowedRequests.isEmpty()) {
                        continue;
                    }
                    List<ToolExecutionResult> results = toolRegistry.executeToolsParallel(allowedRequests);
                    for (int i = 0; i < results.size(); i++) {
                        ToolExecutionResult toolResult = results.get(i);
                        ToolCallRequest request = allowedRequests.get(i);
                        dispatchHook(new StrandsHookEvent.AfterToolCall(iteration, request.toolName(), toolResult));
                        trace = observability.recordIteration(trace, iteration, request.toolName(), request.arguments(),
                                toolResult.output(), Duration.between(iterationStart, Instant.now()));
                        history.add(new ExecutionMessage("tool", toolResult.output()));
                    }
                    continue;
                }

                if (response.hasToolCall()) {
                    ToolCallRequest request = response.toolCallRequest();
                    ToolCallEvaluation eval = evaluateToolCall(iteration, request);
                    if (!eval.allowed()) {
                        ToolExecutionResult deniedResult = new ToolExecutionResult(
                                request.toolName(), false, eval.denialOutput(), Duration.ZERO);
                        dispatchHook(new StrandsHookEvent.AfterToolCall(iteration, request.toolName(), deniedResult));
                        trace = observability.recordIteration(trace, iteration, request.toolName(), request.arguments(),
                                eval.denialOutput(), Duration.between(iterationStart, Instant.now()));
                        history.add(new ExecutionMessage("tool", eval.denialOutput()));
                        continue;
                    }
                    request = eval.request();

                    // Dispatch before-tool-call hook
                    dispatchHook(new StrandsHookEvent.BeforeToolCall(iteration, request.toolName(), request.arguments()));

                    ToolExecutionResult toolResult = toolRegistry.executeTool(request.toolName(), request.arguments());

                    // Dispatch after-tool-call hook
                    dispatchHook(new StrandsHookEvent.AfterToolCall(iteration, request.toolName(), toolResult));

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
                        ToolCallRequest request = boundary.request();
                        ToolCallEvaluation eval = evaluateToolCall(iteration, request);
                        if (!eval.allowed()) {
                            trace = observability.recordIteration(trace, iteration, request.toolName(),
                                    request.arguments(), eval.denialOutput(), Duration.between(iterationStart, Instant.now()));
                            history.add(new ExecutionMessage("tool", eval.denialOutput()));
                        } else {
                            ToolCallRequest controlled = eval.request();
                            ToolExecutionResult toolResult = toolRegistry.executeTool(
                                    controlled.toolName(), controlled.arguments());
                            trace = observability.recordIteration(trace, iteration, controlled.toolName(),
                                    controlled.arguments(), toolResult.output(),
                                    Duration.between(iterationStart, Instant.now()));
                            history.add(new ExecutionMessage("tool", toolResult.output()));
                        }
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

    /**
     * Calls the model with retry logic when retry is enabled.
     */
    private ModelTurnResponse callModelWithRetry(List<ExecutionMessage> prompt, ToolRegistry toolRegistry) {
        if (!retryEnabled || maxRetries <= 0) {
            return modelClient.generate(prompt, toolRegistry.getToolCallbacks());
        }
        Exception lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return modelClient.generate(prompt, toolRegistry.getToolCallbacks());
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    log.warn("Model call failed (attempt {}/{}), retrying in {}ms: {}",
                            attempt + 1, maxRetries + 1, backoffMillis, e.getMessage());
                    try {
                        Thread.sleep(backoffMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new StrandsExecutionException("Retry interrupted", ie, 0, null);
                    }
                }
            }
        }
        throw new StrandsExecutionException(
                "Model call failed after " + (maxRetries + 1) + " attempts", lastException, 0, null);
    }

    /**
     * Applies the conversation manager if one is configured.
     */
    private List<ExecutionMessage> applyConversationManager(List<ExecutionMessage> history, StrandsExecutionContext context) {
        if (conversationManager == null) {
            return history;
        }
        return conversationManager.manage(history, context);
    }

    /**
     * Dispatches a hook event if a hook registry is configured.
     */
    private void dispatchHook(StrandsHookEvent event) {
        if (hookRegistry != null) {
            hookRegistry.dispatch(event);
        }
    }

    private ToolCallEvaluation evaluateToolCall(int iteration, ToolCallRequest request) {
        if (hookRegistry == null) {
            return ToolCallEvaluation.allowed(request);
        }
        ToolCallPolicyDecision decision = hookRegistry.evaluateToolCall(iteration, request.toolName(), request.arguments());
        if (!decision.allowed()) {
            return ToolCallEvaluation.denied(decision.denialOutput());
        }
        return ToolCallEvaluation.allowed(new ToolCallRequest(request.toolName(), decision.arguments()));
    }

    private record ToolCallEvaluation(ToolCallRequest request, boolean allowed, String denialOutput) {
        static ToolCallEvaluation allowed(ToolCallRequest request) {
            return new ToolCallEvaluation(request, true, null);
        }

        static ToolCallEvaluation denied(String denialOutput) {
            return new ToolCallEvaluation(null, false, denialOutput);
        }
    }

    private List<ExecutionMessage> withSystemPrompt(String systemPrompt, List<ExecutionMessage> history) {
        List<ExecutionMessage> merged = new ArrayList<>();
        merged.add(new ExecutionMessage("system", systemPrompt));
        merged.addAll(history);
        return merged;
    }
}
