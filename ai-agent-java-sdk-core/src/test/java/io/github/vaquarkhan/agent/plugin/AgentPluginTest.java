package io.github.vaquarkhan.agent.plugin;

/**
 * @author Vaquar Khan
 */
import io.github.vaquarkhan.agent.AiAgent;
import io.github.vaquarkhan.agent.config.AiAgentProperties;
import io.github.vaquarkhan.agent.execution.ModelTurnResponse;
import io.github.vaquarkhan.agent.execution.AgentExecutionLoop;
import io.github.vaquarkhan.agent.hook.HookRegistry;
import io.github.vaquarkhan.agent.hook.AgentHookEvent;
import io.github.vaquarkhan.agent.observability.AgentObservability;
import io.github.vaquarkhan.agent.support.MockModelClient;
import io.github.vaquarkhan.agent.tool.ToolRegistry;
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

class AgentPluginTest {

    @Test
    void pluginRegistersHookWhenInitCalled() {
        AtomicBoolean hookFired = new AtomicBoolean(false);
        AgentPlugin hookPlugin = new AgentPlugin() {
            @Override
            public void init(AiAgent agent) {
                agent.getHookRegistry().register(AgentHookEvent.BeforeInvocation.class,
                        event -> hookFired.set(true));
            }
        };

        AiAgent agent = createAgent();
        HookRegistry hookRegistry = new HookRegistry();
        agent.setHookRegistry(hookRegistry);
        agent.setPlugins(List.of(hookPlugin));
        agent.initPlugins();

        // Verify the hook was registered
        assertEquals(1, hookRegistry.hookCount(AgentHookEvent.BeforeInvocation.class));
    }

    @Test
    void initPluginsCallsInitOnAllRegisteredPlugins() {
        List<String> initOrder = new ArrayList<>();
        AgentPlugin plugin1 = new AgentPlugin() {
            @Override
            public void init(AiAgent agent) {
                initOrder.add("plugin1");
            }

            @Override
            public String name() {
                return "Plugin1";
            }
        };
        AgentPlugin plugin2 = new AgentPlugin() {
            @Override
            public void init(AiAgent agent) {
                initOrder.add("plugin2");
            }

            @Override
            public String name() {
                return "Plugin2";
            }
        };

        AiAgent agent = createAgent();
        agent.setHookRegistry(new HookRegistry());
        agent.setPlugins(List.of(plugin1, plugin2));
        agent.initPlugins();

        assertEquals(List.of("plugin1", "plugin2"), initOrder);
    }

    @Test
    void pluginsReceiveAgentInstance() {
        AtomicReference<AiAgent> capturedAgent = new AtomicReference<>();
        AgentPlugin plugin = new AgentPlugin() {
            @Override
            public void init(AiAgent agent) {
                capturedAgent.set(agent);
            }
        };

        AiAgent agent = createAgent();
        agent.setHookRegistry(new HookRegistry());
        agent.setPlugins(List.of(plugin));
        agent.initPlugins();

        assertNotNull(capturedAgent.get());
        assertEquals(agent, capturedAgent.get());
    }

    @Test
    void nullPluginListDoesNotCauseErrors() {
        AiAgent agent = createAgent();
        agent.setPlugins(null);
        assertDoesNotThrow(agent::initPlugins);
    }

    @Test
    void emptyPluginListDoesNotCauseErrors() {
        AiAgent agent = createAgent();
        agent.setPlugins(List.of());
        assertDoesNotThrow(agent::initPlugins);
    }

    @Test
    void pluginNameDefaultReturnsClassSimpleName() {
        AgentPlugin anonymousPlugin = new AgentPlugin() {
            @Override
            public void init(AiAgent agent) {
                // no-op
            }
        };
        // Anonymous classes have empty simple name, but the default method uses getSimpleName()
        String name = anonymousPlugin.name();
        assertNotNull(name);

        // Named inner class test
        AgentPlugin namedPlugin = new NamedTestPlugin();
        assertEquals("NamedTestPlugin", namedPlugin.name());
    }

    static class NamedTestPlugin implements AgentPlugin {
        @Override
        public void init(AiAgent agent) {
            // no-op
        }
    }

    private AiAgent createAgent() {
        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.finalAnswer("ok"));
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        AiAgentProperties properties = new AiAgentProperties();
        properties.setModelProvider("openai");
        properties.setModelId("gpt-test");
        ToolRegistry toolRegistry = new ToolRegistry(new LinkedHashMap<>(), properties.getSecurity());
        AgentObservability observability = new AgentObservability(
                new SimpleMeterRegistry(), ObservationRegistry.NOOP, properties.getSecurity());
        return new AiAgent(loop, toolRegistry, properties, List.of(), observability);
    }
}
