package io.github.vaquarkhan.strands.tool;

/**
 * @author Vaquar Khan
 */
import io.github.vaquarkhan.strands.config.AiAgentProperties;
import io.github.vaquarkhan.strands.model.ToolExecutionResult;
import io.github.vaquarkhan.strands.support.TestToolCallback;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRegistryTest {

    @Test
    void executeToolSuccess() {
        ToolRegistry registry = new ToolRegistry(
                Map.of("calc", new TestToolCallback("calc", "calc", args -> "4")),
                security());
        ToolExecutionResult result = registry.executeTool("calc", "{\"x\":2}");
        assertTrue(result.success());
        assertTrue(result.output().contains("4"));
    }

    @Test
    void unknownToolDoesNotLeakState() {
        ToolRegistry registry = new ToolRegistry(Map.of(), security());
        ToolExecutionResult result = registry.executeTool("missing", "{}");
        assertFalse(result.success());
        assertTrue(!result.output().contains("calc"));
    }

    @Test
    void oversizedArgsRejected() {
        AiAgentProperties.Security security = security();
        security.setMaxToolArgumentBytes(2);
        ToolRegistry registry = new ToolRegistry(
                Map.of("calc", new TestToolCallback("calc", "calc", args -> "4")),
                security);
        ToolExecutionResult result = registry.executeTool("calc", "1234");
        assertFalse(result.success());
    }

    @Test
    void toolExceptionContained() {
        ToolRegistry registry = new ToolRegistry(
                Map.of("calc", new TestToolCallback("calc", "calc", args -> {
                    throw new IllegalStateException("boom");
                })),
                security());
        ToolExecutionResult result = registry.executeTool("calc", "{}");
        assertFalse(result.success());
    }

    @Test
    void timeoutEnforced() {
        AiAgentProperties.Security security = security();
        security.setToolTimeoutSeconds(1);
        ToolRegistry registry = new ToolRegistry(
                Map.of("slow", new TestToolCallback("slow", "slow", args -> {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    return "late";
                })),
                security);
        ToolExecutionResult result = registry.executeTool("slow", "{}");
        assertFalse(result.success());
    }

    @Test
    void unsafeToolNameRejected() {
        ToolRegistry registry = new ToolRegistry(Map.of(), security());
        ToolExecutionResult result = registry.executeTool("bad$name", "{}");
        assertFalse(result.success());
    }

    private AiAgentProperties.Security security() {
        return new AiAgentProperties.Security();
    }
}
