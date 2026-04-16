package com.example.spring.ai.strands.agent.plugin;

import com.example.spring.ai.strands.agent.hook.HookAnnotationProcessor;
import com.example.spring.ai.strands.agent.hook.HookRegistry;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Scans a {@link StrandsPlugin} implementation for auto-discoverable features:
 * <ul>
 *   <li>{@link com.example.spring.ai.strands.agent.hook.OnHook @OnHook} annotated methods
 *       are registered as hooks</li>
 *   <li>Methods returning {@link ToolCallbackProvider} are invoked and their tools
 *       can be registered</li>
 * </ul>
 *
 * <p>Called during {@link com.example.spring.ai.strands.agent.StrandsAgent#initPlugins()}.
 *
 * @author Vaquar Khan
 */
public class PluginScanner {

    private static final Logger log = LoggerFactory.getLogger(PluginScanner.class);

    private final HookRegistry hookRegistry;

    /**
     * Creates a plugin scanner with the given hook registry.
     *
     * @param hookRegistry the registry for discovered hooks
     */
    public PluginScanner(HookRegistry hookRegistry) {
        this.hookRegistry = hookRegistry;
    }

    /**
     * Scans the given plugin for @OnHook methods and ToolCallbackProvider methods.
     *
     * @param plugin the plugin to scan
     * @return a result describing what was discovered
     */
    public ScanResult scan(StrandsPlugin plugin) {
        if (plugin == null) {
            return new ScanResult(0, 0);
        }

        // Scan for @OnHook annotated methods
        HookAnnotationProcessor processor = new HookAnnotationProcessor(hookRegistry);
        int hooksRegistered = processor.process(plugin);

        // Scan for methods returning ToolCallbackProvider
        int toolProviders = scanForToolProviders(plugin);

        log.debug("Scanned plugin '{}': {} hooks, {} tool providers",
                plugin.name(), hooksRegistered, toolProviders);
        return new ScanResult(hooksRegistered, toolProviders);
    }

    private int scanForToolProviders(StrandsPlugin plugin) {
        int count = 0;
        Method[] methods = plugin.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (ToolCallbackProvider.class.isAssignableFrom(method.getReturnType())
                    && method.getParameterCount() == 0) {
                try {
                    method.setAccessible(true);
                    Object result = method.invoke(plugin);
                    if (result instanceof ToolCallbackProvider) {
                        count++;
                        log.debug("Found ToolCallbackProvider method '{}' on plugin '{}'",
                                method.getName(), plugin.name());
                    }
                } catch (Exception e) {
                    log.warn("Failed to invoke ToolCallbackProvider method '{}' on plugin '{}': {}",
                            method.getName(), plugin.name(), e.getMessage());
                }
            }
        }
        return count;
    }

    /**
     * Result of scanning a plugin.
     *
     * @param hooksRegistered  number of @OnHook methods registered
     * @param toolProviders    number of ToolCallbackProvider methods found
     */
    public record ScanResult(int hooksRegistered, int toolProviders) {
    }
}
