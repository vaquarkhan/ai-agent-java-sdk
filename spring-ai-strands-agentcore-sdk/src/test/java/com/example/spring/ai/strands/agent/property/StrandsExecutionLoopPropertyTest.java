package com.example.spring.ai.strands.agent.property;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.ModelTurnResponse;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionException;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionLoop;
import com.example.spring.ai.strands.agent.model.TerminationReason;
import com.example.spring.ai.strands.agent.observability.StrandsObservability;
import com.example.spring.ai.strands.agent.support.MockModelClient;
import com.example.spring.ai.strands.agent.support.TestToolCallback;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrandsExecutionLoopPropertyTest {

    // Feature: spring-ai-strands-agentcore-sdk, Property 2: Execution loop multi-turn correctness
    @Property(tries = 100)
    void multiTurnToolExecutionCorrect(@ForAll("turnCount") int turnCount) {
        MockModelClient modelClient = new MockModelClient();
        for (int i = 0; i < turnCount; i++) {
            modelClient.addResponse(ModelTurnResponse.toolCall("calc", "{\"i\":" + i + "}"));
        }
        modelClient.addResponse(ModelTurnResponse.finalAnswer("done"));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, turnCount + 2, "agent");
        var result = loop.run("sys", List.of(new ExecutionMessage("user", "x")),
                toolRegistry(), observability(), StrandsExecutionContext.standalone("s"));
        assertEquals("done", result.content());
        assertEquals(turnCount + 1, result.iterationCount());
    }

    // Feature: spring-ai-strands-agentcore-sdk, Property 3: System prompt inclusion
    @Property(tries = 100)
    void systemPromptIncluded(@ForAll("nonEmpty") String systemPrompt) {
        MockModelClient modelClient = new MockModelClient().addResponse(ModelTurnResponse.finalAnswer("ok"));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 3, "agent");
        loop.run(systemPrompt, List.of(new ExecutionMessage("user", "x")),
                toolRegistry(), observability(), StrandsExecutionContext.standalone("s"));
        assertEquals(systemPrompt, modelClient.getCallHistory().get(0).get(0).content());
    }

    // Feature: spring-ai-strands-agentcore-sdk, Property 4: Max iterations termination
    @Property(tries = 100)
    void maxIterationTermination(@ForAll("maxValues") int maxIterations) {
        MockModelClient modelClient = new MockModelClient();
        for (int i = 0; i < maxIterations; i++) {
            modelClient.addResponse(ModelTurnResponse.toolCall("calc", "{}"));
        }
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, maxIterations, "agent");
        var result = loop.run("sys", List.of(new ExecutionMessage("user", "x")),
                toolRegistry(), observability(), StrandsExecutionContext.standalone("s"));
        assertEquals(TerminationReason.MAX_ITERATIONS_REACHED, result.terminationReason());
        assertEquals(maxIterations, result.iterationCount());
    }

    // Feature: spring-ai-strands-agentcore-sdk, Property 5: Error propagation with iteration number
    @Property(tries = 100)
    void errorPropagationCarriesIteration(@ForAll("failurePoints") int failurePoint) {
        MockModelClient modelClient = new MockModelClient().failAt(i -> i == failurePoint ? new RuntimeException("boom") : null);
        for (int i = 0; i < failurePoint; i++) {
            modelClient.addResponse(ModelTurnResponse.toolCall("calc", "{}"));
        }
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, failurePoint + 1, "agent");
        try {
            loop.run("sys", List.of(new ExecutionMessage("user", "x")),
                    toolRegistry(), observability(), StrandsExecutionContext.standalone("s"));
        } catch (StrandsExecutionException e) {
            assertEquals(failurePoint, e.getIterationNumber());
            return;
        }
        throw new AssertionError("Expected StrandsExecutionException");
    }

    // Feature: spring-ai-strands-agentcore-sdk, Property 10: Reasoning trace completeness
    @Property(tries = 100)
    void traceCompleteness(@ForAll("turnCount") int turnCount) {
        MockModelClient modelClient = new MockModelClient();
        for (int i = 0; i < turnCount; i++) {
            modelClient.addResponse(ModelTurnResponse.toolCall("calc", "{}"));
        }
        modelClient.addResponse(ModelTurnResponse.finalAnswer("done"));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, turnCount + 2, "agent");
        var result = loop.run("sys", List.of(new ExecutionMessage("user", "x")),
                toolRegistry(), observability(), StrandsExecutionContext.standalone("s"));
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
                new StrandsAgentProperties.Security());
    }

    private StrandsObservability observability() {
        return new StrandsObservability(new SimpleMeterRegistry(), ObservationRegistry.NOOP, new StrandsAgentProperties.Security());
    }
}
