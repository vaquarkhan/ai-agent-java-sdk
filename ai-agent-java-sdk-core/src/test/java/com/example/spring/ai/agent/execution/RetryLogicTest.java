package com.example.spring.ai.agent.execution;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.agent.config.AiAgentProperties;
import com.example.spring.ai.agent.observability.AgentObservability;
import com.example.spring.ai.agent.support.MockModelClient;
import com.example.spring.ai.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryLogicTest {

    @Test
    void retryDisabledModelErrorPropagatesImmediately() {
        MockModelClient modelClient = new MockModelClient()
                .failAt(i -> new RuntimeException("model error"));
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        // retry is disabled by default

        AgentExecutionException ex = assertThrows(AgentExecutionException.class, () ->
                loop.run("sys", List.of(new ExecutionMessage("user", "hi")),
                        emptyRegistry(), observability(), AgentExecutionContext.standalone("s")));
        assertTrue(ex.getMessage().contains("Execution failed at iteration 1"));
        // Model should have been called exactly once
        assertEquals(1, modelClient.getCallHistory().size());
    }

    @Test
    void retryEnabledFailingModelRetriedUpToMaxRetries() {
        MockModelClient modelClient = new MockModelClient()
                .failAt(i -> new RuntimeException("model error"));
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        loop.setRetryEnabled(true);
        loop.setMaxRetries(2);
        loop.setBackoffMillis(1); // minimal backoff for test speed

        AgentExecutionException ex = assertThrows(AgentExecutionException.class, () ->
                loop.run("sys", List.of(new ExecutionMessage("user", "hi")),
                        emptyRegistry(), observability(), AgentExecutionContext.standalone("s")));
        // Should have been called 3 times total (1 initial + 2 retries)
        assertEquals(3, modelClient.getCallHistory().size());
        // The retry exception is wrapped by the outer loop's catch block
        assertTrue(ex.getCause().getMessage().contains("Model call failed after 3 attempts"));
    }

    @Test
    void retrySucceedsOnSecondAttempt() {
        // Fail on first call, succeed on second
        MockModelClient modelClient = new MockModelClient()
                .failAt(i -> i == 1 ? new RuntimeException("transient error") : null)
                .addResponse(ModelTurnResponse.finalAnswer("recovered"));
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        loop.setRetryEnabled(true);
        loop.setMaxRetries(2);
        loop.setBackoffMillis(1);

        AgentLoopResult result = loop.run("sys", List.of(new ExecutionMessage("user", "hi")),
                emptyRegistry(), observability(), AgentExecutionContext.standalone("s"));

        assertEquals("recovered", result.content());
        // Called twice: first fails, second succeeds
        assertEquals(2, modelClient.getCallHistory().size());
    }

    @Test
    void retryExhaustionThrowsAgentExecutionExceptionWithCorrectMessage() {
        MockModelClient modelClient = new MockModelClient()
                .failAt(i -> new RuntimeException("persistent failure"));
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        loop.setRetryEnabled(true);
        loop.setMaxRetries(1);
        loop.setBackoffMillis(1);

        AgentExecutionException ex = assertThrows(AgentExecutionException.class, () ->
                loop.run("sys", List.of(new ExecutionMessage("user", "hi")),
                        emptyRegistry(), observability(), AgentExecutionContext.standalone("s")));
        // 1 initial + 1 retry = 2 total attempts
        assertEquals(2, modelClient.getCallHistory().size());
        // The retry exception is wrapped by the outer loop's catch block
        assertTrue(ex.getCause().getMessage().contains("Model call failed after 2 attempts"));
    }

    private ToolRegistry emptyRegistry() {
        return new ToolRegistry(Map.of(), new AiAgentProperties.Security());
    }

    private AgentObservability observability() {
        return new AgentObservability(new SimpleMeterRegistry(), ObservationRegistry.NOOP,
                new AiAgentProperties.Security());
    }
}
