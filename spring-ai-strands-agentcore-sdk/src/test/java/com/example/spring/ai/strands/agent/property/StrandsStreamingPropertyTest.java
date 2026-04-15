package com.example.spring.ai.strands.agent.property;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionLoop;
import com.example.spring.ai.strands.agent.execution.ToolCallRequest;
import com.example.spring.ai.strands.agent.execution.stream.StreamEvent;
import com.example.spring.ai.strands.agent.observability.StrandsObservability;
import com.example.spring.ai.strands.agent.support.MockModelClient;
import com.example.spring.ai.strands.agent.support.TestToolCallback;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StrandsStreamingPropertyTest {

    // Feature: spring-ai-strands-agentcore-sdk, Property 13: Streaming token order preservation
    @Property(tries = 100)
    void tokenOrderPreserved(@ForAll("tokens") List<String> tokens) {
        MockModelClient modelClient = new MockModelClient().addStreamResponse(withComplete(tokens));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 3, "agent");
        List<String> emitted = new ArrayList<>();
        StepVerifier.create(loop.runStreaming("sys", List.of(new ExecutionMessage("user", "x")),
                        registry(), observability(), StrandsExecutionContext.standalone("s"))
                .doOnNext(emitted::add))
                .expectNextCount(tokens.size())
                .verifyComplete();
        assertEquals(String.join("", tokens), String.join("", emitted));
    }

    // Feature: spring-ai-strands-agentcore-sdk, Property 14: Streaming pause and resume for tool calls
    @Property(tries = 100)
    void pauseResumeForToolCalls(@ForAll("tokens") List<String> tokens) {
        MockModelClient modelClient = new MockModelClient()
                .addStreamResponse(List.of(
                        new StreamEvent.Token(tokens.get(0)),
                        new StreamEvent.ToolCallBoundary(new ToolCallRequest("calc", "{}")),
                        new StreamEvent.Complete("")))
                .addStreamResponse(withComplete(tokens.subList(1, tokens.size())));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 3, "agent");
        StepVerifier.create(loop.runStreaming("sys", List.of(new ExecutionMessage("user", "x")),
                        registry(), observability(), StrandsExecutionContext.standalone("s")))
                .expectNextCount(tokens.size())
                .verifyComplete();
    }

    // Feature: spring-ai-strands-agentcore-sdk, Property 15: Streaming error signal
    @Property(tries = 100)
    void streamingErrorSignal(@ForAll("failurePoint") int failurePoint) {
        MockModelClient modelClient = new MockModelClient().failAt(i -> i >= failurePoint ? new RuntimeException("boom") : null)
                .addStreamResponse(List.of(new StreamEvent.Token("x"), new StreamEvent.Complete("x")));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 3, "agent");
        StepVerifier.create(loop.runStreaming("sys", List.of(new ExecutionMessage("user", "x")),
                        registry(), observability(), StrandsExecutionContext.standalone("s")))
                .expectError()
                .verify();
    }

    @net.jqwik.api.Provide
    net.jqwik.api.Arbitrary<List<String>> tokens() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(3).list().ofMinSize(2).ofMaxSize(8);
    }

    @net.jqwik.api.Provide
    net.jqwik.api.Arbitrary<Integer> failurePoint() {
        return Arbitraries.integers().between(1, 1);
    }

    private List<StreamEvent> withComplete(List<String> tokens) {
        List<StreamEvent> events = new ArrayList<>();
        StringBuilder all = new StringBuilder();
        for (String token : tokens) {
            events.add(new StreamEvent.Token(token));
            all.append(token);
        }
        events.add(new StreamEvent.Complete(all.toString()));
        return events;
    }

    private ToolRegistry registry() {
        return new ToolRegistry(Map.of("calc", new TestToolCallback("calc", "c", a -> "4")),
                new StrandsAgentProperties.Security());
    }

    private StrandsObservability observability() {
        return new StrandsObservability(new SimpleMeterRegistry(), ObservationRegistry.NOOP, new StrandsAgentProperties.Security());
    }
}
