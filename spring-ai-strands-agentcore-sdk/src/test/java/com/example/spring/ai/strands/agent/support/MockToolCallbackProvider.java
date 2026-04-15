package com.example.spring.ai.strands.agent.support;

/**
 * @author Vaquar Khan
 */
import java.util.List;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

public class MockToolCallbackProvider implements ToolCallbackProvider {

    private final List<ToolCallback> callbacks;

    public MockToolCallbackProvider(List<ToolCallback> callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public FunctionCallback[] getToolCallbacks() {
        return callbacks.toArray(FunctionCallback[]::new);
    }
}
