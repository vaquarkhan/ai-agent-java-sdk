package io.github.vaquarkhan.strands.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.vaquarkhan.strands.approval.ManualApprovalManager;
import io.github.vaquarkhan.strands.config.AiAgentProperties;
import io.github.vaquarkhan.strands.observability.AgentObservability;
import io.github.vaquarkhan.strands.support.MockModelClient;
import io.github.vaquarkhan.strands.support.TestToolCallback;
import io.github.vaquarkhan.strands.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ExecutionLoopApprovalTest {

    @Test
    void toolExecutionWaitsForApprovalAndResumes() throws Exception {
        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.toolCall("calc", "{\"x\":2}"))
                .addResponse(ModelTurnResponse.finalAnswer("done"));
        AtomicReference<String> seenArguments = new AtomicReference<>();
        ToolRegistry registry = new ToolRegistry(
                Map.of("calc", new TestToolCallback("calc", "calculator", a -> {
                    seenArguments.set(a);
                    return "4";
                })),
                new AiAgentProperties.Security());
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        ManualApprovalManager approvalManager = new ManualApprovalManager(Duration.ofSeconds(5));
        loop.setApprovalManager(approvalManager);

        AtomicReference<AgentLoopResult> resultRef = new AtomicReference<>();
        Thread t = new Thread(() -> resultRef.set(loop.run(
                "sys",
                java.util.List.of(new ExecutionMessage("user", "2+2")),
                registry,
                observability(),
                AgentExecutionContext.standalone("s"))));
        t.start();

        PendingWait.waitUntil(() -> !approvalManager.getPendingApprovals().isEmpty(), 2000);
        var pending = approvalManager.getPendingApprovals().iterator().next();
        assertNotNull(pending);
        approvalManager.approve(pending.requestId(), "{\"x\":99}");

        t.join(3000);
        assertFalse(t.isAlive());
        assertEquals("{\"x\":99}", seenArguments.get());
        assertEquals("done", resultRef.get().content());
    }

    @Test
    void deniedApprovalFeedsErrorBackToLoop() throws Exception {
        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.toolCall("calc", "{\"x\":2}"))
                .addResponse(ModelTurnResponse.finalAnswer("done"));
        ToolRegistry registry = new ToolRegistry(
                Map.of("calc", new TestToolCallback("calc", "calculator", a -> {
                    throw new IllegalStateException("tool should not execute");
                })),
                new AiAgentProperties.Security());
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        ManualApprovalManager approvalManager = new ManualApprovalManager(Duration.ofSeconds(5));
        loop.setApprovalManager(approvalManager);

        AtomicReference<AgentLoopResult> resultRef = new AtomicReference<>();
        Thread t = new Thread(() -> resultRef.set(loop.run(
                "sys",
                java.util.List.of(new ExecutionMessage("user", "2+2")),
                registry,
                observability(),
                AgentExecutionContext.standalone("s"))));
        t.start();

        PendingWait.waitUntil(() -> !approvalManager.getPendingApprovals().isEmpty(), 2000);
        var pending = approvalManager.getPendingApprovals().iterator().next();
        approvalManager.deny(pending.requestId(), "{\"error\":\"approval_required\"}");

        t.join(3000);
        assertFalse(t.isAlive());
        assertEquals("done", resultRef.get().content());
    }

    @Test
    void approvalTimeoutDeniesToolCall() {
        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.toolCall("calc", "{\"x\":2}"))
                .addResponse(ModelTurnResponse.finalAnswer("done"));
        ToolRegistry registry = new ToolRegistry(
                Map.of("calc", new TestToolCallback("calc", "calculator", a -> {
                    throw new IllegalStateException("tool should not execute");
                })),
                new AiAgentProperties.Security());
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        loop.setApprovalManager(new ManualApprovalManager(Duration.ofMillis(50)));

        AgentLoopResult result = loop.run(
                "sys",
                java.util.List.of(new ExecutionMessage("user", "2+2")),
                registry,
                observability(),
                AgentExecutionContext.standalone("s"));

        assertEquals("done", result.content());
    }

    private AgentObservability observability() {
        return new AgentObservability(new SimpleMeterRegistry(), ObservationRegistry.NOOP,
                new AiAgentProperties.Security());
    }

    private static final class PendingWait {
        private static void waitUntil(java.util.function.BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMillis;
            while (System.currentTimeMillis() < deadline) {
                if (condition.getAsBoolean()) {
                    return;
                }
                Thread.sleep(10);
            }
            throw new AssertionError("Timed out waiting for pending approval");
        }
    }
}
