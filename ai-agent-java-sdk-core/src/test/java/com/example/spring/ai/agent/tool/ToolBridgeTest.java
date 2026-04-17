package com.example.spring.ai.agent.tool;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.agent.config.AiAgentProperties;
import com.example.spring.ai.agent.support.MockToolCallbackProvider;
import com.example.spring.ai.agent.support.TestAiAgentProperties;
import com.example.spring.ai.agent.support.TestToolCallback;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolBridgeTest {

    @Test
    void discoveryFromMultipleProviders() {
        AiAgentProperties p = TestAiAgentProperties.minimal();
        var registry = ToolBridge.discoverTools(
                List.of(
                        new MockToolCallbackProvider(List.of(new TestToolCallback("calc", "c", a -> "1"))),
                        new MockToolCallbackProvider(List.of(new TestToolCallback("weather", "w", a -> "sunny")))),
                p);
        assertEquals(2, registry.size());
    }

    @Test
    void includeFilterApplied() {
        AiAgentProperties.ToolDiscovery discovery = new AiAgentProperties.ToolDiscovery();
        discovery.setIncludePatterns(List.of("calc*"));
        AiAgentProperties p = TestAiAgentProperties.withToolDiscovery(discovery);
        var registry = ToolBridge.discoverTools(
                List.of(new MockToolCallbackProvider(
                        List.of(
                                new TestToolCallback("calc", "c", a -> "1"),
                                new TestToolCallback("weather", "w", a -> "sunny")))),
                p);
        assertTrue(registry.hasTool("calc"));
        assertFalse(registry.hasTool("weather"));
    }

    @Test
    void denyOverAllowPrecedence() {
        AiAgentProperties.ToolDiscovery discovery = new AiAgentProperties.ToolDiscovery();
        discovery.setIncludePatterns(List.of("*"));
        discovery.setExcludePatterns(List.of("calc*"));
        AiAgentProperties p = TestAiAgentProperties.withToolDiscovery(discovery);
        var registry = ToolBridge.discoverTools(
                List.of(new MockToolCallbackProvider(List.of(new TestToolCallback("calc", "c", a -> "1")))), p);
        assertFalse(registry.hasTool("calc"));
    }

    @Test
    void emptyProviderProducesEmptyRegistry() {
        AiAgentProperties p = TestAiAgentProperties.minimal();
        var registry = ToolBridge.discoverTools(List.of(), p);
        assertEquals(0, registry.size());
    }
}
