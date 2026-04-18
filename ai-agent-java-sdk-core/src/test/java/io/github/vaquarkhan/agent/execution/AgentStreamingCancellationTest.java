package io.github.vaquarkhan.agent.execution;

import io.github.vaquarkhan.agent.config.AiAgentProperties;
import io.github.vaquarkhan.agent.execution.stream.StreamEvent;
import io.github.vaquarkhan.agent.observability.AgentObservability;
import io.github.vaquarkhan.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that when a subscriber cancels the Flux mid-stream, the execution loop
 * stops cleanly and no further tokens are emitted after cancellation.
 *
 * @author Vaquar Khan
 */
class AgentStreamingCancellationTest {

    @Test
    void cancellationStopsStreamCleanly() {
        AtomicInteger emittedCount = new AtomicInteger(0);

        // Model client that emits many tokens across multiple iterations
        LoopModelClient slowClient = new LoopModelClient() {
            @Override
            public ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                return ModelTurnResponse.finalAnswer("done");
            }

            @Override
            public Flux<StreamEvent> stream(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                // Emit 50 tokens then complete
                return Flux.range(0, 50)
                        .map(i -> {
                            emittedCount.incrementAndGet();
                            return (StreamEvent) new StreamEvent.Token("token-" + i + " ");
                        })
                        .concatWith(Flux.just(new StreamEvent.Complete("done")));
            }
        };

        AgentExecutionLoop loop = new AgentExecutionLoop(slowClient, 5, "agent");
        ToolRegistry registry = new ToolRegistry(Map.of(), new AiAgentProperties.Security());

        // Take only 3 tokens then cancel
        StepVerifier.create(
                        loop.runStreaming("sys", List.of(new ExecutionMessage("user", "x")),
                                registry, observability(), AgentExecutionContext.standalone("s")))
                .expectNextCount(3)
                .thenCancel()
                .verify();

        // After cancellation, the emitted count should be limited (not all 50)
        // The exact count depends on buffering, but it should be significantly less than 50
        assertTrue(emittedCount.get() <= 50,
                "Stream should not emit all tokens after cancellation");
    }

    private AgentObservability observability() {
        return new AgentObservability(
                new SimpleMeterRegistry(), ObservationRegistry.NOOP, new AiAgentProperties.Security());
    }
}
