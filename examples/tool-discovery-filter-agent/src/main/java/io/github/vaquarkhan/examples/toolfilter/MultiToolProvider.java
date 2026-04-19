package io.github.vaquarkhan.examples.toolfilter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;

/**
 * Registers four tool names; {@code application.yml} filters which names reach {@link io.github.vaquarkhan.strands.tool.ToolRegistry}.
 *
 * @author Vaquar Khan
 */
@Component
public class MultiToolProvider implements ToolCallbackProvider {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public ToolCallback[] getToolCallbacks() {
        return new ToolCallback[] {
            namedEcho("admin_secret", "Admin-only (should be excluded by glob)."),
            namedEcho("public_echo", "Echo for public namespace."),
            namedEcho("demo_calc", "Multiply a*b - JSON {\"a\":int,\"b\":int}."),
            namedEcho("alpha_nav", "Navigation alpha (should be excluded by include allowlist).")
        };
    }

    private ToolCallback namedEcho(String name, String description) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                        .name(name)
                        .description(description)
                        .inputSchema("{\"type\":\"object\"}")
                        .build();
            }

            @Override
            public String call(String arguments) {
                if ("demo_calc".equals(name)) {
                    try {
                        JsonNode n = JSON.readTree(arguments);
                        long a = n.path("a").asLong();
                        long b = n.path("b").asLong();
                        return JSON.writeValueAsString(java.util.Map.of("product", a * b));
                    } catch (Exception e) {
                        return "{\"error\":\"bad_args\"}";
                    }
                }
                ObjectNode out = JSON.createObjectNode();
                out.put("tool", name);
                out.put("echo", arguments == null ? "" : arguments);
                return out.toString();
            }
        };
    }
}
