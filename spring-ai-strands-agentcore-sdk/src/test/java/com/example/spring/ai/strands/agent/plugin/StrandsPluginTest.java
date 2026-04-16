package com.example.spring.ai.strands.agent.plugin;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.StrandsAgent;
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.execution.ModelTurnResponse;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionLoop;
import com.example.spring.ai.strands.agent.hook.HookRegistry;
import com.example.spring.ai.strands.agent.hook.StrandsHookEvent;
import com.example.spring.ai.strands.agent.observability.StrandsObservability;
import com.example.spring.ai.strands.agent.support.MockModelClient;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrandsPluginTest {

    @Test
    void pluginRegistersHookWhenInitCalled() {
        AtomicBoolean hookFired = new AtomicBoolean(false);
        StrandsPlugin hookPlugin = new StrandsPlugin() {
            @Override
            public void init(StrandsAgent agent) {
                agent.getHookRegistry().register(StrandsHookEvent.BeforeInvocation.class,
                        event -> hookFired.set(true));
            }
        };

        StrandsAgent agent = createAgent();
        HookRegistry hookRegistry = new HookRegistry();
        agent.setHookRegistry(hookRegistry);
        agent.setPlugins(List.of(hookPlugin));
        agent.initPlugins();

        // Verify the hook was registered
        assertEquals(1, hookRegistry.hookCount(StrandsHookEvent.BeforeInvocation.class));
    }

    @Test
    void initPluginsCallsInitOnAllRegisteredPlugins() {
        List<String> initOrder = new ArrayList<>();
        StrandsPlugin plugin1 = new StrandsPlugin() {
            @Override
            public void init(StrandsAgent agent) {
                initOrder.add("plugin1");
            }

            @Override
            public String name() {
                return "Plugin1";
            }
        };
        StrandsPlugin plugin2 = new StrandsPlugin() {
            @Override
            public void init(StrandsAgent agent) {
                initOrder.add("plugin2");
            }

            @Override
            public String name() {
                return "Plugin2";
            }
        };

        StrandsAgent agent = createAgent();
        agent.setHookRegistry(new HookRegistry());
        agent.setPlugins(List.of(plugin1, plugin2));
        agent.initPlugins();

        assertEquals(List.of("plugin1", "plugin2"), initOrder);
    }

    @Test
    void pluginsReceiveAgentInstance() {
        AtomicReference<StrandsAgent> capturedAgent = new AtomicReference<>();
        StrandsPlugin plugin = new StrandsPlugin() {
            @Override
            public void init(StrandsAgent agent) {
                capturedAgent.set(agent);
            }
        };

        StrandsAgent agent = createAgent();
        agent.setHookRegistry(new HookRegistry());
        agent.setPlugins(List.of(plugin));
        agent.initPlugins();

        assertNotNull(capturedAgent.get());
        assertEquals(agent, capturedAgent.get());
    }

    @Test
    void nullPluginListDoesNotCauseErrors() {
        StrandsAgent agent = createAgent();
        agent.setPlugins(null);
        assertDoesNotThrow(agent::initPlugins);
    }

    @Test
    void emptyPluginListDoesNotCauseErrors() {
        StrandsAgent agent = createAgent();
        agent.setPlugins(List.of());
        assertDoesNotThrow(agent::initPlugins);
    }

    @Test
    void pluginNameDefaultReturnsClassSimpleName() {
        StrandsPlugin anonymousPlugin = new StrandsPlugin() {
            @Override
            public void init(StrandsAgent agent) {
                // no-op
            }
        };
        // Anonymous classes have empty simple name, but the default method uses getSimpleName()
        String name = anonymousPlugin.name();
        assertNotNull(name);

        // Named inner class test
        StrandsPlugin namedPlugin = new NamedTestPlugin();
        assertEquals("NamedTestPlugin", namedPlugin.name());
    }

    static class NamedTestPlugin implements StrandsPlugin {
        @Override
        public void init(StrandsAgent agent) {
            // no-op
        }
    }

    private StrandsAgent createAgent() {
        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.finalAnswer("ok"));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        StrandsAgentProperties properties = new StrandsAgentProperties();
        properties.setModelProvider("openai");
        properties.setModelId("gpt-test");
        ToolRegistry toolRegistry = new ToolRegistry(new LinkedHashMap<>(), properties.getSecurity());
        StrandsObservability observability = new StrandsObservability(
                new SimpleMeterRegistry(), ObservationRegistry.NOOP, properties.getSecurity());
        return new StrandsAgent(loop, toolRegistry, properties, List.of(), observability);
    }
}
