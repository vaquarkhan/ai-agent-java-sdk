package com.example.spring.ai.strands.agent.security;

import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.ModelTurnResponse;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionLoop;
import com.example.spring.ai.strands.agent.execution.StrandsLoopResult;
import com.example.spring.ai.strands.agent.observability.StrandsObservability;
import com.example.spring.ai.strands.agent.support.MockModelClient;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that two concurrent executions with different session IDs cannot access
 * each other's context. Each thread runs its own execution loop with a distinct
 * {@link StrandsExecutionContext} and the session IDs remain isolated throughout.
 *
 * @author Vaquar Khan
 */
class StrandsSessionIsolationTest {

    @Test
    void concurrentSessionsRemainIsolated() throws InterruptedException {
        Map<String, String> results = new ConcurrentHashMap<>();
        Map<String, String> sessionIds = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(2);

        Runnable task1 = () -> {
            try {
                MockModelClient client = new MockModelClient()
                        .addResponse(ModelTurnResponse.finalAnswer("response-for-session-A"));
                StrandsExecutionLoop loop = new StrandsExecutionLoop(client, 5, "agent");
                StrandsExecutionContext context = StrandsExecutionContext.standalone("session-A");
                StrandsLoopResult result = loop.run("sys", List.of(new ExecutionMessage("user", "q1")),
                        emptyRegistry(), observability(), context);
                results.put("thread1-content", result.content());
                sessionIds.put("thread1-session", result.reasoningTrace().sessionId());
            } finally {
                latch.countDown();
            }
        };

        Runnable task2 = () -> {
            try {
                MockModelClient client = new MockModelClient()
                        .addResponse(ModelTurnResponse.finalAnswer("response-for-session-B"));
                StrandsExecutionLoop loop = new StrandsExecutionLoop(client, 5, "agent");
                StrandsExecutionContext context = StrandsExecutionContext.standalone("session-B");
                StrandsLoopResult result = loop.run("sys", List.of(new ExecutionMessage("user", "q2")),
                        emptyRegistry(), observability(), context);
                results.put("thread2-content", result.content());
                sessionIds.put("thread2-session", result.reasoningTrace().sessionId());
            } finally {
                latch.countDown();
            }
        };

        Thread t1 = new Thread(task1, "session-A-thread");
        Thread t2 = new Thread(task2, "session-B-thread");
        t1.start();
        t2.start();

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Both threads should complete within timeout");

        // Each thread got its own response
        assertEquals("response-for-session-A", results.get("thread1-content"));
        assertEquals("response-for-session-B", results.get("thread2-content"));

        // Session IDs are isolated
        assertEquals("session-A", sessionIds.get("thread1-session"));
        assertEquals("session-B", sessionIds.get("thread2-session"));
        assertNotEquals(sessionIds.get("thread1-session"), sessionIds.get("thread2-session"));
    }

    private ToolRegistry emptyRegistry() {
        return new ToolRegistry(Map.of(), new StrandsAgentProperties.Security());
    }

    private StrandsObservability observability() {
        return new StrandsObservability(
                new SimpleMeterRegistry(), ObservationRegistry.NOOP, new StrandsAgentProperties.Security());
    }
}
