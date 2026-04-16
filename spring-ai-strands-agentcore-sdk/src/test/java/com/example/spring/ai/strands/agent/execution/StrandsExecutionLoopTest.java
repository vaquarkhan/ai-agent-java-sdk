package com.example.spring.ai.strands.agent.execution;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.execution.stream.StreamEvent;
import com.example.spring.ai.strands.agent.hook.HookRegistry;
import com.example.spring.ai.strands.agent.hook.ToolCallPolicyDecision;
import com.example.spring.ai.strands.agent.model.TerminationReason;
import com.example.spring.ai.strands.agent.observability.StrandsObservability;
import com.example.spring.ai.strands.agent.support.MockModelClient;
import com.example.spring.ai.strands.agent.support.TestToolCallback;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrandsExecutionLoopTest {

    @Test
    void zeroIterationFinalAnswer() {
        MockModelClient modelClient = new MockModelClient().addResponse(ModelTurnResponse.finalAnswer("done"));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        StrandsLoopResult result = loop.run("sys", List.of(new ExecutionMessage("user", "hi")),
                emptyRegistry(), observability(), StrandsExecutionContext.standalone("s"));
        assertEquals("done", result.content());
        assertEquals(TerminationReason.MODEL_COMPLETION, result.terminationReason());
    }

    @Test
    void singleToolCallThenFinalAnswer() {
        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.toolCall("calc", "{\"x\":2}"))
                .addResponse(ModelTurnResponse.finalAnswer("done"));
        ToolRegistry registry = new ToolRegistry(
                Map.of("calc", new TestToolCallback("calc", "c", a -> "4")),
                new StrandsAgentProperties.Security());
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        StrandsLoopResult result = loop.run("sys", List.of(new ExecutionMessage("user", "2+2")),
                registry, observability(), StrandsExecutionContext.standalone("s"));
        assertEquals("done", result.content());
        assertEquals(2, result.iterationCount());
    }

    @Test
    void maxIterationsTerminated() {
        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.toolCall("calc", "{}"))
                .addResponse(ModelTurnResponse.toolCall("calc", "{}"));
        ToolRegistry registry = new ToolRegistry(
                Map.of("calc", new TestToolCallback("calc", "c", a -> "4")),
                new StrandsAgentProperties.Security());
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 2, "agent");
        StrandsLoopResult result = loop.run("sys", List.of(new ExecutionMessage("user", "x")),
                registry, observability(), StrandsExecutionContext.standalone("s"));
        assertEquals(TerminationReason.MAX_ITERATIONS_REACHED, result.terminationReason());
    }

    @Test
    void modelErrorWrappedWithIteration() {
        MockModelClient modelClient = new MockModelClient().failAt(i -> i == 2 ? new RuntimeException("boom") : null)
                .addResponse(ModelTurnResponse.toolCall("calc", "{}"));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 4, "agent");
        assertThrows(StrandsExecutionException.class, () -> loop.run("sys", List.of(new ExecutionMessage("user", "x")),
                emptyRegistry(), observability(), StrandsExecutionContext.standalone("s")));
    }

    @Test
    void systemPromptIncludedEveryModelCall() {
        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.toolCall("calc", "{}"))
                .addResponse(ModelTurnResponse.finalAnswer("ok"));
        ToolRegistry registry = new ToolRegistry(
                Map.of("calc", new TestToolCallback("calc", "c", a -> "4")),
                new StrandsAgentProperties.Security());
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 3, "agent");
        loop.run("SYSTEM", List.of(new ExecutionMessage("user", "x")),
                registry, observability(), StrandsExecutionContext.standalone("s"));
        assertTrue(modelClient.getCallHistory().stream()
                .allMatch(messages -> messages.get(0).role().equals("system")
                        && messages.get(0).content().equals("SYSTEM")));
    }

    @Test
    void streamingPauseResumeAndCompletion() {
        MockModelClient modelClient = new MockModelClient()
                .addStreamResponse(List.of(
                        new StreamEvent.Token("hel"),
                        new StreamEvent.ToolCallBoundary(new ToolCallRequest("calc", "{}")),
                        new StreamEvent.Complete("")))
                .addStreamResponse(List.of(
                        new StreamEvent.Token("lo"),
                        new StreamEvent.Complete("hello")));
        ToolRegistry registry = new ToolRegistry(
                Map.of("calc", new TestToolCallback("calc", "c", a -> "4")),
                new StrandsAgentProperties.Security());
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 3, "agent");
        StepVerifier.create(loop.runStreaming("sys", List.of(new ExecutionMessage("user", "x")),
                        registry, observability(), StrandsExecutionContext.standalone("s")))
                .expectNext("hel", "lo")
                .verifyComplete();
    }

    @Test
    void toolPolicyCanDenyToolExecution() {
        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.toolCall("calc", "{\"x\":2}"))
                .addResponse(ModelTurnResponse.finalAnswer("done"));
        ToolRegistry registry = new ToolRegistry(
                Map.of("calc", new TestToolCallback("calc", "c", a -> {
                    throw new IllegalStateException("tool should have been denied");
                })),
                new StrandsAgentProperties.Security());
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        HookRegistry hookRegistry = new HookRegistry();
        hookRegistry.registerToolCallPolicy((iteration, toolName, arguments) ->
                ToolCallPolicyDecision.deny("{\"error\":\"approval_required\"}"));
        loop.setHookRegistry(hookRegistry);

        StrandsLoopResult result = loop.run("sys", List.of(new ExecutionMessage("user", "2+2")),
                registry, observability(), StrandsExecutionContext.standalone("s"));
        assertEquals("done", result.content());
    }

    @Test
    void toolPolicyCanRewriteArguments() {
        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.toolCall("calc", "{\"x\":2}"))
                .addResponse(ModelTurnResponse.finalAnswer("done"));
        AtomicReference<String> seenArguments = new AtomicReference<>();
        ToolRegistry registry = new ToolRegistry(
                Map.of("calc", new TestToolCallback("calc", "c", a -> {
                    seenArguments.set(a);
                    return "4";
                })),
                new StrandsAgentProperties.Security());
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        HookRegistry hookRegistry = new HookRegistry();
        hookRegistry.registerToolCallPolicy((iteration, toolName, arguments) ->
                ToolCallPolicyDecision.allow("{\"x\":99}"));
        loop.setHookRegistry(hookRegistry);

        StrandsLoopResult result = loop.run("sys", List.of(new ExecutionMessage("user", "2+2")),
                registry, observability(), StrandsExecutionContext.standalone("s"));
        assertEquals("done", result.content());
        assertEquals("{\"x\":99}", seenArguments.get());
    }

    private ToolRegistry emptyRegistry() {
        return new ToolRegistry(Map.of(), new StrandsAgentProperties.Security());
    }

    private StrandsObservability observability() {
        return new StrandsObservability(new SimpleMeterRegistry(), ObservationRegistry.NOOP, new StrandsAgentProperties.Security());
    }
}
