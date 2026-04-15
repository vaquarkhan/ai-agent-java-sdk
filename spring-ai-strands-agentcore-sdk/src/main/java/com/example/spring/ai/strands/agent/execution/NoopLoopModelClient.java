package com.example.spring.ai.strands.agent.execution;

import com.example.spring.ai.strands.agent.execution.stream.StreamEvent;
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
