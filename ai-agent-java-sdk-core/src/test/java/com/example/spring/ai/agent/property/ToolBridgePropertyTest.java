package com.example.spring.ai.agent.property;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.agent.config.AiAgentProperties;
import com.example.spring.ai.agent.support.MockToolCallbackProvider;
import com.example.spring.ai.agent.support.TestAiAgentProperties;
import com.example.spring.ai.agent.support.TestToolCallback;
import com.example.spring.ai.agent.tool.ToolBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolBridgePropertyTest {

    @Property(tries = 100)
    void toolDiscoveryCompleteness(@ForAll("providerSizes") List<Integer> providerSizes) {
        List<MockToolCallbackProvider> providers = new ArrayList<>();
        int expected = 0;
        for (int i = 0; i < providerSizes.size(); i++) {
            int count = providerSizes.get(i);
            expected += count;
            final int providerIndex = i;
            List<TestToolCallback> callbacks = IntStream.range(0, count)
                    .mapToObj(j -> new TestToolCallback("tool_" + providerIndex + "_" + j, "d", a -> "ok"))
                    .toList();
            providers.add(new MockToolCallbackProvider(new ArrayList<>(callbacks)));
        }
        AiAgentProperties p = TestAiAgentProperties.minimal();
        var registry = ToolBridge.discoverTools(new ArrayList<>(providers), p);
        assertEquals(expected, registry.size());
    }

    @Property(tries = 100)
    void metadataPreserved(@ForAll("safeToolNames") String toolName, @ForAll("safeToolNames") String description) {
        var callback = new TestToolCallback(toolName, description, a -> "ok");
        AiAgentProperties p = TestAiAgentProperties.minimal();
        var registry = ToolBridge.discoverTools(List.of(new MockToolCallbackProvider(List.of(callback))), p);
        assertEquals(toolName, registry.getToolDefinitions().get(0).name());
        assertEquals(description, registry.getToolDefinitions().get(0).description());
    }

    @Property(tries = 100)
    void filteringRespectsDenyOverAllow(@ForAll("safeToolNames") String toolName) {
        AiAgentProperties.ToolDiscovery discovery = new AiAgentProperties.ToolDiscovery();
        discovery.setIncludePatterns(List.of("*"));
        discovery.setExcludePatterns(List.of(toolName));
        AiAgentProperties p = TestAiAgentProperties.withToolDiscovery(discovery);
        var registry = ToolBridge.discoverTools(
                List.of(new MockToolCallbackProvider(List.of(new TestToolCallback(toolName, "d", a -> "ok")))), p);
        assertTrue(!registry.hasTool(toolName));
    }

    @Provide
    Arbitrary<List<Integer>> providerSizes() {
        return Arbitraries.integers().between(0, 5).list().ofMinSize(1).ofMaxSize(5);
    }

    @Provide
    Arbitrary<String> safeToolNames() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(3).ofMaxLength(12);
    }
}
