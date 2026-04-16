package com.example.spring.ai.strands.agent.tool;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.execution.ToolCallRequest;
import com.example.spring.ai.strands.agent.model.ToolExecutionResult;
import com.example.spring.ai.strands.agent.support.TestToolCallback;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelToolExecutionTest {

    @Test
    void parallelExecutionOfThreeToolsReturnsResultsInOrder() {
        ToolRegistry registry = new ToolRegistry(Map.of(
                "add", new TestToolCallback("add", "addition", a -> "sum"),
                "mul", new TestToolCallback("mul", "multiply", a -> "product"),
                "sub", new TestToolCallback("sub", "subtract", a -> "difference")),
                new StrandsAgentProperties.Security());
        registry.beginNewLoop();

        List<ToolExecutionResult> results = registry.executeToolsParallel(List.of(
                new ToolCallRequest("add", "{}"),
                new ToolCallRequest("mul", "{}"),
                new ToolCallRequest("sub", "{}")));

        assertEquals(3, results.size());
        assertEquals("add", results.get(0).toolName());
        assertEquals("sum", results.get(0).output());
        assertEquals("mul", results.get(1).toolName());
        assertEquals("product", results.get(1).output());
        assertEquals("sub", results.get(2).toolName());
        assertEquals("difference", results.get(2).output());
    }

    @Test
    void parallelExecutionWithOneToolFailingOthersSucceed() {
        ToolRegistry registry = new ToolRegistry(Map.of(
                "good", new TestToolCallback("good", "works", a -> "ok"),
                "bad", new TestToolCallback("bad", "fails", a -> { throw new RuntimeException("boom"); }),
                "also-good", new TestToolCallback("also-good", "works too", a -> "fine")),
                new StrandsAgentProperties.Security());
        registry.beginNewLoop();

        List<ToolExecutionResult> results = registry.executeToolsParallel(List.of(
                new ToolCallRequest("good", "{}"),
                new ToolCallRequest("bad", "{}"),
                new ToolCallRequest("also-good", "{}")));

        assertEquals(3, results.size());
        assertTrue(results.get(0).success());
        assertEquals("ok", results.get(0).output());
        assertFalse(results.get(1).success());
        assertTrue(results.get(2).success());
        assertEquals("fine", results.get(2).output());
    }

    @Test
    void parallelExecutionWithSingleToolDelegatesToExecuteTool() {
        ToolRegistry registry = new ToolRegistry(Map.of(
                "calc", new TestToolCallback("calc", "calculator", a -> "42")),
                new StrandsAgentProperties.Security());
        registry.beginNewLoop();

        List<ToolExecutionResult> results = registry.executeToolsParallel(
                List.of(new ToolCallRequest("calc", "{}")));

        assertEquals(1, results.size());
        assertTrue(results.get(0).success());
        assertEquals("42", results.get(0).output());
    }

    @Test
    void parallelExecutionWithEmptyListReturnsEmptyList() {
        ToolRegistry registry = new ToolRegistry(Map.of(),
                new StrandsAgentProperties.Security());
        registry.beginNewLoop();

        List<ToolExecutionResult> results = registry.executeToolsParallel(List.of());
        assertTrue(results.isEmpty());
    }

    @Test
    void parallelExecutionRespectsRateLimits() {
        StrandsAgentProperties.Security security = new StrandsAgentProperties.Security();
        security.setToolRateLimit(2);
        ToolRegistry registry = new ToolRegistry(Map.of(
                "a", new TestToolCallback("a", "a", arg -> "ok-a"),
                "b", new TestToolCallback("b", "b", arg -> "ok-b"),
                "c", new TestToolCallback("c", "c", arg -> "ok-c")),
                security);
        registry.beginNewLoop();

        List<ToolExecutionResult> results = registry.executeToolsParallel(List.of(
                new ToolCallRequest("a", "{}"),
                new ToolCallRequest("b", "{}"),
                new ToolCallRequest("c", "{}")));

        assertEquals(3, results.size());
        long rateLimited = results.stream()
                .filter(r -> !r.success() && r.output().contains("rate_limit"))
                .count();
        assertTrue(rateLimited >= 1, "At least one tool should be rate-limited");
    }

    @Test
    void parallelExecutionRespectsTimeouts() {
        StrandsAgentProperties.Security security = new StrandsAgentProperties.Security();
        security.setToolTimeoutSeconds(1);
        ToolRegistry registry = new ToolRegistry(Map.of(
                "slow", new TestToolCallback("slow", "slow tool", a -> {
                    try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return "late";
                }),
                "fast", new TestToolCallback("fast", "fast tool", a -> "quick")),
                security);
        registry.beginNewLoop();

        List<ToolExecutionResult> results = registry.executeToolsParallel(List.of(
                new ToolCallRequest("slow", "{}"),
                new ToolCallRequest("fast", "{}")));

        assertEquals(2, results.size());
        assertFalse(results.get(0).success());
        assertTrue(results.get(0).output().contains("timeout"));
        assertTrue(results.get(1).success());
        assertEquals("quick", results.get(1).output());
    }
}
