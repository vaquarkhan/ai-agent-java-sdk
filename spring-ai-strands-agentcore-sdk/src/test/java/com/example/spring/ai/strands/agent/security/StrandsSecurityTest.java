package com.example.spring.ai.strands.agent.security;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.config.StrandsAgentPropertiesValidator;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.observability.StrandsObservability;
import com.example.spring.ai.strands.agent.support.MockToolCallbackProvider;
import com.example.spring.ai.strands.agent.support.TestStrandsProperties;
import com.example.spring.ai.strands.agent.support.TestToolCallback;
import com.example.spring.ai.strands.agent.tool.ToolBridge;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.UrlResource;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrandsSecurityTest {

    @Test
    void argumentLimitRateLimitAndUnknownToolHandled() {
        StrandsAgentProperties.Security security = new StrandsAgentProperties.Security();
        security.setMaxToolArgumentBytes(3);
        security.setToolRateLimit(2);
        ToolRegistry registry = new ToolRegistry(
                new LinkedHashMap<>(
                        Map.of(
                                "calc",
                                new TestToolCallback("calc", "c", a -> "4"))),
                security);
        registry.beginNewLoop();
        assertFalse(registry.executeTool("calc", "1234").success());
        assertTrue(registry.executeTool("calc", "{}").success());
        assertTrue(registry.executeTool("calc", "{}").success());
        assertFalse(registry.executeTool("calc", "{}").success());
        assertFalse(registry.executeTool("missing", "{}").success());
    }

    @Test
    void slowToolTimesOut() {
        StrandsAgentProperties.Security security = new StrandsAgentProperties.Security();
        security.setToolTimeoutSeconds(1);
        ToolRegistry registry = new ToolRegistry(
                new LinkedHashMap<>(
                        Map.of(
                                "slow",
                                new TestToolCallback("slow", "s", a -> {
                                    try {
                                        Thread.sleep(1200);
                                    } catch (InterruptedException ignored) {
                                        Thread.currentThread().interrupt();
                                    }
                                    return "x";
                                }))),
                security);
        registry.beginNewLoop();
        assertFalse(registry.executeTool("slow", "{}").success());
    }

    @Test
    void contextAndHeadersProtected() {
        StrandsExecutionContext context = new StrandsExecutionContext("s", "u", Map.of("Authorization", "secret"));
        assertFalse(context.toString().contains("secret"));
        assertThrows(UnsupportedOperationException.class, () -> context.getHeaders().put("x", "y"));
    }

    @Test
    void sanitizerAndTruncationAndSsrfValidation() throws Exception {
        StrandsAgentProperties.Security security = new StrandsAgentProperties.Security();
        security.setSanitizeToolOutput(true);
        security.setTraceMaxOutputLength(12);
        StrandsObservability observability =
                new StrandsObservability(new SimpleMeterRegistry(), ObservationRegistry.NOOP, security);
        var trace = observability.startTrace("agent", StrandsExecutionContext.standalone("s"));
        trace = observability.recordIteration(
                trace,
                1,
                "tool",
                "in",
                "AKIA1234567890123456 user@example.com token.abc.def",
                Duration.ofMillis(1));
        assertTrue(trace.iterations().get(0).toolOutput().contains("REDACTED"));
        assertTrue(trace.iterations().get(0).toolOutput().length() <= 12);

        StrandsAgentProperties properties = new StrandsAgentProperties();
        properties.setModelProvider("openai");
        properties.setModelId("gpt");
        properties.setSystemPromptResource(new UrlResource("https://evil.local/prompt.txt"));
        assertThrows(IllegalArgumentException.class, () -> StrandsAgentPropertiesValidator.validateOrThrow(properties));
    }

    @Test
    void toolNameValidationAndDenyOverAllow() {
        StrandsAgentProperties.ToolDiscovery discovery = new StrandsAgentProperties.ToolDiscovery();
        discovery.setIncludePatterns(List.of("*"));
        discovery.setExcludePatterns(List.of("safe_tool"));
        StrandsAgentProperties p = TestStrandsProperties.withToolDiscovery(discovery);
        var registry = ToolBridge.discoverTools(
                List.of(
                        new MockToolCallbackProvider(
                                List.of(
                                        new TestToolCallback("safe_tool", "d", a -> "x"),
                                        new TestToolCallback("bad$name", "d", a -> "x")))),
                p);
        assertFalse(registry.hasTool("safe_tool"));
        assertFalse(registry.hasTool("bad$name"));
    }
}
