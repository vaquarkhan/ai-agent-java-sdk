package io.github.vaquarkhan.strands.property;

/**
 * @author Vaquar Khan
 */
import io.github.vaquarkhan.strands.config.AiAgentProperties;
import io.github.vaquarkhan.strands.execution.ExecutionMessage;
import io.github.vaquarkhan.strands.execution.ModelTurnResponse;
import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
import io.github.vaquarkhan.strands.execution.AgentExecutionException;
import io.github.vaquarkhan.strands.execution.AgentExecutionLoop;
import io.github.vaquarkhan.strands.model.TerminationReason;
import io.github.vaquarkhan.strands.observability.AgentObservability;
import io.github.vaquarkhan.strands.support.MockModelClient;
import io.github.vaquarkhan.strands.support.TestToolCallback;
import io.github.vaquarkhan.strands.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentExecutionLoopPropertyTest {

    // Feature: ai-agent-java-sdk-core, Property 2: Execution loop multi-turn correctness
    @Property(tries = 100)
    void multiTurnToolExecutionCorrect(@ForAll("turnCount") int turnCount) {
        MockModelClient modelClient = new MockModelClient();
        for (int i = 0; i < turnCount; i++) {
            modelClient.addResponse(ModelTurnResponse.toolCall("calc", "{\"i\":" + i + "}"));
        }
        modelClient.addResponse(ModelTurnResponse.finalAnswer("done"));
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, turnCount + 2, "agent");
        var result = loop.run("sys", List.of(new ExecutionMessage("user", "x")),
                toolRegistry(), observability(), AgentExecutionContext.standalone("s"));
        assertEquals("done", result.content());
        assertEquals(turnCount + 1, result.iterationCount());
    }

    // Feature: ai-agent-java-sdk-core, Property 3: System prompt inclusion
    @Property(tries = 100)
    void systemPromptIncluded(@ForAll("nonEmpty") String systemPrompt) {
        MockModelClient modelClient = new MockModelClient().addResponse(ModelTurnResponse.finalAnswer("ok"));
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 3, "agent");
        loop.run(systemPrompt, List.of(new ExecutionMessage("user", "x")),
                toolRegistry(), observability(), AgentExecutionContext.standalone("s"));
        assertEquals(systemPrompt, modelClient.getCallHistory().get(0).get(0).content());
    }

    // Feature: ai-agent-java-sdk-core, Property 4: Max iterations termination
    @Property(tries = 100)
    void maxIterationTermination(@ForAll("maxValues") int maxIterations) {
        MockModelClient modelClient = new MockModelClient();
        for (int i = 0; i < maxIterations; i++) {
            modelClient.addResponse(ModelTurnResponse.toolCall("calc", "{}"));
        }
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, maxIterations, "agent");
        var result = loop.run("sys", List.of(new ExecutionMessage("user", "x")),
                toolRegistry(), observability(), AgentExecutionContext.standalone("s"));
        assertEquals(TerminationReason.MAX_ITERATIONS_REACHED, result.terminationReason());
        assertEquals(maxIterations, result.iterationCount());
    }

    // Feature: ai-agent-java-sdk-core, Property 5: Error propagation with iteration number
    @Property(tries = 100)
    void errorPropagationCarriesIteration(@ForAll("failurePoints") int failurePoint) {
        MockModelClient modelClient = new MockModelClient().failAt(i -> i == failurePoint ? new RuntimeException("boom") : null);
        for (int i = 0; i < failurePoint; i++) {
            modelClient.addResponse(ModelTurnResponse.toolCall("calc", "{}"));
        }
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, failurePoint + 1, "agent");
        try {
            loop.run("sys", List.of(new ExecutionMessage("user", "x")),
                    toolRegistry(), observability(), AgentExecutionContext.standalone("s"));
        } catch (AgentExecutionException e) {
            assertEquals(failurePoint, e.getIterationNumber());
            return;
        }
        throw new AssertionError("Expected AgentExecutionException");
    }

    // Feature: ai-agent-java-sdk-core, Property 10: Reasoning trace completeness
    @Property(tries = 100)
    void traceCompleteness(@ForAll("turnCount") int turnCount) {
        MockModelClient modelClient = new MockModelClient();
        for (int i = 0; i < turnCount; i++) {
            modelClient.addResponse(ModelTurnResponse.toolCall("calc", "{}"));
        }
        modelClient.addResponse(ModelTurnResponse.finalAnswer("done"));
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, turnCount + 2, "agent");
        var result = loop.run("sys", List.of(new ExecutionMessage("user", "x")),
                toolRegistry(), observability(), AgentExecutionContext.standalone("s"));
        assertEquals(turnCount + 1, result.reasoningTrace().iterations().size());
        assertTrue(result.reasoningTrace().iterations().stream().allMatch(r -> !r.elapsed().isNegative()));
    }

    @net.jqwik.api.Provide
    net.jqwik.api.Arbitrary<Integer> turnCount() {
        return Arbitraries.integers().between(0, 10);
    }

    @net.jqwik.api.Provide
    net.jqwik.api.Arbitrary<String> nonEmpty() {
        return Arbitraries.strings().ascii().ofMinLength(1).ofMaxLength(30);
    }

    @net.jqwik.api.Provide
    net.jqwik.api.Arbitrary<Integer> maxValues() {
        return Arbitraries.integers().between(1, 20);
    }

    @net.jqwik.api.Provide
    net.jqwik.api.Arbitrary<Integer> failurePoints() {
        return Arbitraries.integers().between(1, 8);
    }

    private ToolRegistry toolRegistry() {
        return new ToolRegistry(Map.of("calc", new TestToolCallback("calc", "c", a -> "4")),
                new AiAgentProperties.Security());
    }

    private AgentObservability observability() {
        return new AgentObservability(new SimpleMeterRegistry(), ObservationRegistry.NOOP, new AiAgentProperties.Security());
    }
}
