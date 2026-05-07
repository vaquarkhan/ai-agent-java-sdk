package io.github.vaquarkhan.strands.plugin;

import io.github.vaquarkhan.strands.AiAgent;
import io.github.vaquarkhan.strands.hook.HookRegistry;
import io.github.vaquarkhan.strands.hook.OnHook;
import io.github.vaquarkhan.strands.hook.AgentHookEvent;
import io.github.vaquarkhan.strands.support.TestToolCallback;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Vaquar Khan
 */
class PluginScannerTest {

    @Test
    void scanPluginWithOnHookMethods() {
        HookRegistry registry = new HookRegistry();
        PluginScanner scanner = new PluginScanner(registry);

        PluginScanner.ScanResult result = scanner.scan(new HookPlugin());

        assertEquals(1, result.hooksRegistered());
        assertEquals(1, registry.hookCount(AgentHookEvent.BeforeModelCall.class));
    }

    @Test
    void scanPluginWithToolCallbackProviderMethod() {
        HookRegistry registry = new HookRegistry();
        PluginScanner scanner = new PluginScanner(registry);

        PluginScanner.ScanResult result = scanner.scan(new ToolProviderPlugin());

        assertEquals(1, result.toolProviders());
    }

    @Test
    void scanPluginWithNoAnnotations() {
        HookRegistry registry = new HookRegistry();
        PluginScanner scanner = new PluginScanner(registry);

        PluginScanner.ScanResult result = scanner.scan(new EmptyPlugin());

        assertEquals(0, result.hooksRegistered());
        assertEquals(0, result.toolProviders());
    }

    @Test
    void scanPluginWithBothHooksAndToolProviders() {
        HookRegistry registry = new HookRegistry();
        PluginScanner scanner = new PluginScanner(registry);

        PluginScanner.ScanResult result = scanner.scan(new FullPlugin());

        assertEquals(1, result.hooksRegistered());
        assertEquals(1, result.toolProviders());
    }

    @Test
    void scanNullPluginReturnsZeros() {
        HookRegistry registry = new HookRegistry();
        PluginScanner scanner = new PluginScanner(registry);

        PluginScanner.ScanResult result = scanner.scan(null);

        assertEquals(0, result.hooksRegistered());
        assertEquals(0, result.toolProviders());
    }

    @Test
    void scanResultRecordAccessors() {
        PluginScanner.ScanResult result = new PluginScanner.ScanResult(3, 2);
        assertEquals(3, result.hooksRegistered());
        assertEquals(2, result.toolProviders());
    }

    // --- Test plugins ---

    static class HookPlugin implements AgentPlugin {
        @Override
        public void init(AiAgent agent) {
            // no-op
        }

        @OnHook(AgentHookEvent.BeforeModelCall.class)
        public void onBeforeModel(AgentHookEvent event) {
            // hook handler
        }
    }

    static class ToolProviderPlugin implements AgentPlugin {
        @Override
        public void init(AiAgent agent) {
            // no-op
        }

        public ToolCallbackProvider myTools() {
            return () -> new ToolCallback[] {
                    new TestToolCallback("plugin_tool", "A plugin tool", args -> "result")
            };
        }
    }

    static class EmptyPlugin implements AgentPlugin {
        @Override
        public void init(AiAgent agent) {
            // no-op
        }
    }

    static class FullPlugin implements AgentPlugin {
        @Override
        public void init(AiAgent agent) {
            // no-op
        }

        @OnHook(AgentHookEvent.AfterToolCall.class)
        public void onAfterTool(AgentHookEvent event) {
            // hook handler
        }

        public ToolCallbackProvider tools() {
            return () -> new ToolCallback[] {
                    new TestToolCallback("full_tool", "Full plugin tool", args -> "ok")
            };
        }
    }
}
