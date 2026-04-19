package io.github.vaquarkhan.strands.examples.streaming;

import io.github.vaquarkhan.strands.execution.ExecutionMessage;
import io.github.vaquarkhan.strands.execution.LoopModelClient;
import io.github.vaquarkhan.strands.execution.ModelTurnResponse;
import io.github.vaquarkhan.strands.execution.stream.StreamEvent;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

/**
 * Scripted streaming {@link LoopModelClient} that emits tokens one word at a time,
 * simulating how a real LLM streams partial responses.
 *
 * Swap this bean with a ChatClient-backed implementation for real LLM streaming.
 *
 * @author Vaquar Khan
 */
@Configuration
public class StreamingAgentConfiguration {

    private static final String SCRIPTED_RESPONSE =
            "Spring AI provides a familiar Spring-style programming model "
                    + "for building AI-powered applications in Java.";

    @Bean
    @Primary
    public LoopModelClient streamingScriptedModelClient() {
        return new LoopModelClient() {

            @Override
            public ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                return ModelTurnResponse.finalAnswer(SCRIPTED_RESPONSE);
            }

            @Override
            public Flux<StreamEvent> stream(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                // Emit each word as a separate token, simulating real LLM streaming
                String[] words = SCRIPTED_RESPONSE.split("(?<=\\s)");
                Flux<StreamEvent> tokens = Flux.fromArray(words).map(StreamEvent.Token::new);
                Flux<StreamEvent> complete = Flux.just(new StreamEvent.Complete(SCRIPTED_RESPONSE));
                return Flux.concat(tokens, complete);
            }
        };
    }
}
