package io.github.vaquarkhan.strands.support;

/**
 * @author Vaquar Khan
 */
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

public class MockToolCallbackProvider implements ToolCallbackProvider {

    private final List<ToolCallback> callbacks;

    public MockToolCallbackProvider(List<ToolCallback> callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return callbacks.toArray(ToolCallback[]::new);
    }
}
