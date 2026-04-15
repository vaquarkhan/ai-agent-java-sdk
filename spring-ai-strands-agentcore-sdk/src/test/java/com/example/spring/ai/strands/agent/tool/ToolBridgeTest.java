package com.example.spring.ai.strands.agent.tool;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.support.MockToolCallbackProvider;
import com.example.spring.ai.strands.agent.support.TestStrandsProperties;
import com.example.spring.ai.strands.agent.support.TestToolCallback;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolBridgeTest {

    @Test
    void discoveryFromMultipleProviders() {
        StrandsAgentProperties p = TestStrandsProperties.minimal();
        var registry = ToolBridge.discoverTools(
                List.of(
                        new MockToolCallbackProvider(List.of(new TestToolCallback("calc", "c", a -> "1"))),
                        new MockToolCallbackProvider(List.of(new TestToolCallback("weather", "w", a -> "sunny")))),
                p);
        assertEquals(2, registry.size());
    }

    @Test
    void includeFilterApplied() {
        StrandsAgentProperties.ToolDiscovery discovery = new StrandsAgentProperties.ToolDiscovery();
        discovery.setIncludePatterns(List.of("calc*"));
        StrandsAgentProperties p = TestStrandsProperties.withToolDiscovery(discovery);
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
        StrandsAgentProperties.ToolDiscovery discovery = new StrandsAgentProperties.ToolDiscovery();
        discovery.setIncludePatterns(List.of("*"));
        discovery.setExcludePatterns(List.of("calc*"));
        StrandsAgentProperties p = TestStrandsProperties.withToolDiscovery(discovery);
        var registry = ToolBridge.discoverTools(
                List.of(new MockToolCallbackProvider(List.of(new TestToolCallback("calc", "c", a -> "1")))), p);
        assertFalse(registry.hasTool("calc"));
    }

    @Test
    void emptyProviderProducesEmptyRegistry() {
        StrandsAgentProperties p = TestStrandsProperties.minimal();
        var registry = ToolBridge.discoverTools(List.of(), p);
        assertEquals(0, registry.size());
    }
}
