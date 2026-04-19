package io.github.vaquarkhan.strands.execution;

import io.github.vaquarkhan.strands.execution.stream.StreamEvent;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

/**
 * @author Vaquar Khan
 */

public class NoopLoopModelClient implements LoopModelClient {

    @Override
    public ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools) {
        throw new UnsupportedOperationException("No model client configured. Provide a LoopModelClient bean.");
    }

    @Override
    public Flux<StreamEvent> stream(List<ExecutionMessage> messages, List<ToolCallback> tools) {
        return Flux.error(new UnsupportedOperationException("No model client configured. Provide a LoopModelClient bean."));
    }
}
