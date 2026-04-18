package io.github.vaquarkhan.agent.support;

/**
 * @author Vaquar Khan
 */
import java.util.function.Function;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

public class TestToolCallback implements ToolCallback {

    private final ToolDefinition definition;
    private final Function<String, String> behavior;

    public TestToolCallback(String name, String description, Function<String, String> behavior) {
        this.definition = DefaultToolDefinition.builder()
                .name(name)
                .description(description)
                .inputSchema("{\"type\":\"object\"}")
                .build();
        this.behavior = behavior;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return definition;
    }

    @Override
    public String call(String arguments) {
        return behavior.apply(arguments);
    }
}
