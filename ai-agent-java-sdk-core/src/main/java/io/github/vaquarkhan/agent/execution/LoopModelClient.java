package io.github.vaquarkhan.agent.execution;

import io.github.vaquarkhan.agent.execution.stream.StreamEvent;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

/**
 * @author Vaquar Khan
 */

public interface LoopModelClient {

    ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools);

    Flux<StreamEvent> stream(List<ExecutionMessage> messages, List<ToolCallback> tools);
}
