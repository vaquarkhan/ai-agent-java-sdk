package com.example.spring.ai.strands.agent.tool;

import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.model.ToolExecutionResult;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Registry of tools available to the execution loop, with size limits, timeouts, and rate limits.
 *
 * @author Vaquar Khan
 */
public final class ToolRegistry {

    private static final Pattern SAFE_TOOL_NAME = Pattern.compile("[a-zA-Z0-9_-]+");
    private static final String UNKNOWN_TOOL_MESSAGE = "{\"error\":\"tool_not_found\"}";
    private static final String RATE_LIMIT_MESSAGE = "{\"error\":\"tool_rate_limit\"}";

    private final Map<String, ToolCallback> tools;
    private final StrandsAgentProperties.Security security;
    private int callsThisLoop;

    public ToolRegistry(Map<String, ToolCallback> tools, StrandsAgentProperties.Security security) {
        this.tools = Collections.unmodifiableMap(new LinkedHashMap<>(tools));
        this.security = Objects.requireNonNull(security, "security");
    }

    /** Reset per-loop counters (call at the start of each execution loop run). */
    public void beginNewLoop() {
        this.callsThisLoop = 0;
    }

    public List<ToolDefinition> getToolDefinitions() {
        return tools.values().stream().map(ToolCallback::getToolDefinition).toList();
    }

    public List<ToolCallback> getToolCallbacks() {
        return tools.values().stream().toList();
    }

    public int size() {
        return tools.size();
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    public ToolExecutionResult executeTool(String toolName, String arguments) {
        if (!SAFE_TOOL_NAME.matcher(toolName).matches()) {
            return new ToolExecutionResult(
                    toolName,
                    false,
                    "{\"error\":\"invalid_tool_name\"}",
                    Duration.ZERO);
        }
        ToolCallback callback = tools.get(toolName);
        if (callback == null) {
            return new ToolExecutionResult(
                    toolName,
                    false,
                    UNKNOWN_TOOL_MESSAGE,
                    Duration.ZERO);
        }
        int max = security.getToolRateLimit();
        if (max > 0 && callsThisLoop >= max) {
            return new ToolExecutionResult(
                    toolName,
                    false,
                    RATE_LIMIT_MESSAGE,
                    Duration.ZERO);
        }
        byte[] argBytes = arguments == null ? new byte[0] : arguments.getBytes(StandardCharsets.UTF_8);
        if (argBytes.length > security.getMaxToolArgumentBytes()) {
            return new ToolExecutionResult(
                    toolName,
                    false,
                    "{\"error\":\"tool_argument_too_large\"}",
                    Duration.ZERO);
        }
        callsThisLoop++;
        Instant start = Instant.now();
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "strands-tool-" + toolName);
            t.setDaemon(true);
            return t;
        });
        try {
            Future<String> future = executor.submit(() -> callback.call(arguments));
            String out = future.get(security.getToolTimeoutSeconds(), TimeUnit.SECONDS);
            Duration elapsed = Duration.between(start, Instant.now());
            return new ToolExecutionResult(toolName, true, out, elapsed);
        } catch (TimeoutException e) {
            return new ToolExecutionResult(
                    toolName,
                    false,
                    "{\"error\":\"tool_timeout\"}",
                    Duration.between(start, Instant.now()));
        } catch (Exception e) {
            return new ToolExecutionResult(
                    toolName,
                    false,
                    "{\"error\":\"tool_execution_failed\"}",
                    Duration.between(start, Instant.now()));
        } finally {
            executor.shutdownNow();
        }
    }
}
