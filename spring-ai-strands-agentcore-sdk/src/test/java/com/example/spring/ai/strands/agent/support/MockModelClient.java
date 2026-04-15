package com.example.spring.ai.strands.agent.support;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.LoopModelClient;
import com.example.spring.ai.strands.agent.execution.ModelTurnResponse;
import com.example.spring.ai.strands.agent.execution.stream.StreamEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

public class MockModelClient implements LoopModelClient {

    private final Deque<ModelTurnResponse> responses = new ArrayDeque<>();
    private final Deque<List<StreamEvent>> streamResponses = new ArrayDeque<>();
    private final List<List<ExecutionMessage>> callHistory = new ArrayList<>();
    private Function<Integer, RuntimeException> failureAtIteration;
    private int iterationCounter = 0;

    public MockModelClient addResponse(ModelTurnResponse response) {
        responses.add(response);
        return this;
    }

    public MockModelClient addStreamResponse(List<StreamEvent> response) {
        streamResponses.add(response);
        return this;
    }

    public MockModelClient failAt(Function<Integer, RuntimeException> failureAtIteration) {
        this.failureAtIteration = failureAtIteration;
        return this;
    }

    @Override
    public ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools) {
        iterationCounter++;
        callHistory.add(List.copyOf(messages));
        if (failureAtIteration != null) {
            RuntimeException runtimeException = failureAtIteration.apply(iterationCounter);
            if (runtimeException != null) {
                throw runtimeException;
            }
        }
        return responses.removeFirst();
    }

    @Override
    public Flux<StreamEvent> stream(List<ExecutionMessage> messages, List<ToolCallback> tools) {
        iterationCounter++;
        callHistory.add(List.copyOf(messages));
        if (failureAtIteration != null) {
            RuntimeException runtimeException = failureAtIteration.apply(iterationCounter);
            if (runtimeException != null) {
                return Flux.error(runtimeException);
            }
        }
        return Flux.fromIterable(streamResponses.removeFirst());
    }

    public List<List<ExecutionMessage>> getCallHistory() {
        return callHistory;
    }
}
