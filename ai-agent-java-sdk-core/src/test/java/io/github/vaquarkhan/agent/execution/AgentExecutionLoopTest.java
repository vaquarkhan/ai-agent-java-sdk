package io.github.vaquarkhan.agent.execution;

/**
 * @author Vaquar Khan
 */
import io.github.vaquarkhan.agent.config.AiAgentProperties;
import io.github.vaquarkhan.agent.execution.stream.StreamEvent;
import io.github.vaquarkhan.agent.hook.HookRegistry;
import io.github.vaquarkhan.agent.hook.ToolCallPolicyDecision;
import io.github.vaquarkhan.agent.model.TerminationReason;
import io.github.vaquarkhan.agent.observability.AgentObservability;
import io.github.vaquarkhan.agent.support.MockModelClient;
import io.github.vaquarkhan.agent.support.TestToolCallback;
import io.github.vaquarkhan.agent.tool.ToolRegistry;
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

class AgentExecutionLoopTest {

    @Test
    void zeroIterationFinalAnswer() {
        MockModelClient modelClient = new MockModelClient().addResponse(ModelTurnResponse.finalAnswer("done"));
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        AgentLoopResult result = loop.run("sys", List.of(new ExecutionMessage("user", "hi")),
                emptyRegistry(), observability(), AgentExecutionContext.standalone("s"));
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
                new AiAgentProperties.Security());
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        AgentLoopResult result = loop.run("sys", List.of(new ExecutionMessage("user", "2+2")),
                registry, observability(), AgentExecutionContext.standalone("s"));
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
                new AiAgentProperties.Security());
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 2, "agent");
        AgentLoopResult result = loop.run("sys", List.of(new ExecutionMessage("user", "x")),
                registry, observability(), AgentExecutionContext.standalone("s"));
        assertEquals(TerminationReason.MAX_ITERATIONS_REACHED, result.terminationReason());
    }

    @Test
    void modelErrorWrappedWithIteration() {
        MockModelClient modelClient = new MockModelClient().failAt(i -> i == 2 ? new RuntimeException("boom") : null)
                .addResponse(ModelTurnResponse.toolCall("calc", "{}"));
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 4, "agent");
        assertThrows(AgentExecutionException.class, () -> loop.run("sys", List.of(new ExecutionMessage("user", "x")),
                emptyRegistry(), observability(), AgentExecutionContext.standalone("s")));
    }

    @Test
    void systemPromptIncludedEveryModelCall() {
        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.toolCall("calc", "{}"))
                .addResponse(ModelTurnResponse.finalAnswer("ok"));
        ToolRegistry registry = new ToolRegistry(
                Map.of("calc", new TestToolCallback("calc", "c", a -> "4")),
                new AiAgentProperties.Security());
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 3, "agent");
        loop.run("SYSTEM", List.of(new ExecutionMessage("user", "x")),
                registry, observability(), AgentExecutionContext.standalone("s"));
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
                new AiAgentProperties.Security());
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 3, "agent");
        StepVerifier.create(loop.runStreaming("sys", List.of(new ExecutionMessage("user", "x")),
                        registry, observability(), AgentExecutionContext.standalone("s")))
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
                new AiAgentProperties.Security());
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        HookRegistry hookRegistry = new HookRegistry();
        hookRegistry.registerToolCallPolicy((iteration, toolName, arguments) ->
                ToolCallPolicyDecision.deny("{\"error\":\"approval_required\"}"));
        loop.setHookRegistry(hookRegistry);

        AgentLoopResult result = loop.run("sys", List.of(new ExecutionMessage("user", "2+2")),
                registry, observability(), AgentExecutionContext.standalone("s"));
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
                new AiAgentProperties.Security());
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        HookRegistry hookRegistry = new HookRegistry();
        hookRegistry.registerToolCallPolicy((iteration, toolName, arguments) ->
                ToolCallPolicyDecision.allow("{\"x\":99}"));
        loop.setHookRegistry(hookRegistry);

        AgentLoopResult result = loop.run("sys", List.of(new ExecutionMessage("user", "2+2")),
                registry, observability(), AgentExecutionContext.standalone("s"));
        assertEquals("done", result.content());
        assertEquals("{\"x\":99}", seenArguments.get());
    }

    private ToolRegistry emptyRegistry() {
        return new ToolRegistry(Map.of(), new AiAgentProperties.Security());
    }

    private AgentObservability observability() {
        return new AgentObservability(new SimpleMeterRegistry(), ObservationRegistry.NOOP, new AiAgentProperties.Security());
    }
}
