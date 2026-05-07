package io.github.vaquarkhan.strands.tool;

import io.github.vaquarkhan.strands.config.AiAgentProperties;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Discovers {@link ToolCallback} instances from {@link ToolCallbackProvider} beans and applies
 * include/exclude glob patterns (deny-over-allow: exclude wins).
 *
 * @author Vaquar Khan
 */
public final class ToolBridge {

    private static final Logger log = LoggerFactory.getLogger(ToolBridge.class);
    private static final Pattern SAFE_TOOL_NAME = Pattern.compile("[a-zA-Z0-9_-]+");

    private ToolBridge() {}

    public static ToolRegistry discoverTools(
            List<ToolCallbackProvider> providers, AiAgentProperties properties) {
        AiAgentProperties.ToolDiscovery discovery = properties.getToolDiscovery();
        AiAgentProperties.Security security = properties.getSecurity();
        Map<String, ToolCallback> map = new LinkedHashMap<>();
        if (!discovery.isEnabled()) {
            return new ToolRegistry(map, security);
        }
        if (providers == null || providers.isEmpty()) {
            return new ToolRegistry(map, security);
        }
        List<String> include = discovery.getIncludePatterns();
        List<String> exclude = discovery.getExcludePatterns();
        for (ToolCallbackProvider provider : providers) {
            for (ToolCallback callback : provider.getToolCallbacks()) {
                String name = callback.getToolDefinition().name();
                if (!SAFE_TOOL_NAME.matcher(name).matches()) {
                    log.warn("Skipping tool with invalid name: {}", name);
                    continue;
                }
                if (!matches(name, include, exclude)) {
                    continue;
                }
                if (map.containsKey(name)) {
                    log.warn("Duplicate tool name {}, skipping duplicate", name);
                    continue;
                }
                map.put(name, callback);
            }
        }
        return new ToolRegistry(map, security);
    }

    static boolean matches(String toolName, List<String> includePatterns, List<String> excludePatterns) {
        FileSystem fs = FileSystems.getDefault();
        for (String pattern : excludePatterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            PathMatcher matcher = fs.getPathMatcher("glob:" + pattern);
            if (matcher.matches(Path.of(toolName))) {
                return false;
            }
        }
        if (includePatterns == null || includePatterns.isEmpty()) {
            return true;
        }
        for (String pattern : includePatterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            PathMatcher matcher = fs.getPathMatcher("glob:" + pattern);
            if (matcher.matches(Path.of(toolName))) {
                return true;
            }
        }
        return false;
    }
}
