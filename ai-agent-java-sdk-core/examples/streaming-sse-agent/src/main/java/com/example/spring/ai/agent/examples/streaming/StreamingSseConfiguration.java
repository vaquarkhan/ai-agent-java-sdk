package com.example.spring.ai.agent.examples.streaming;

import com.example.spring.ai.agent.execution.ExecutionMessage;
import com.example.spring.ai.agent.execution.LoopModelClient;
import com.example.spring.ai.agent.execution.ModelTurnResponse;
import com.example.spring.ai.agent.execution.stream.StreamEvent;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

/**
 * No tools; scripted stream emits token chunks then completes (offline-friendly).
 *
 * @author Vaquar Khan
 */
@Configuration
public class StreamingSseConfiguration {

    @Bean
    @Primary
    public LoopModelClient streamingDemoModel() {
        return new LoopModelClient() {
            @Override
            public ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                return ModelTurnResponse.finalAnswer(
                        "Streaming demo: model output is delivered as token events over SSE.");
            }

            @Override
            public Flux<StreamEvent> stream(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                return Flux.just(
                        new StreamEvent.Token("Streaming"),
                        new StreamEvent.Token(" demo:"),
                        new StreamEvent.Token(" tokens"),
                        new StreamEvent.Token(" arrive"),
                        new StreamEvent.Token(" incrementally."),
                        new StreamEvent.Complete(
                                "Streaming demo: tokens arrive incrementally."));
            }
        };
    }
}
