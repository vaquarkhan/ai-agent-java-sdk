package io.github.vaquarkhan.agent.examples.minimal;

import io.github.vaquarkhan.agent.execution.ExecutionMessage;
import io.github.vaquarkhan.agent.execution.LoopModelClient;
import io.github.vaquarkhan.agent.execution.ModelTurnResponse;
import io.github.vaquarkhan.agent.execution.stream.StreamEvent;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

/**
 * Scripted {@link LoopModelClient} that echoes the user message back as a final answer.
 * No tools, no multi-turn reasoning - the simplest possible agent loop.
 *
 * Swap this bean with a ChatClient-backed implementation for real LLM behavior.
 *
 * @author Vaquar Khan
 */
@Configuration
public class MinimalAgentConfiguration {

    @Bean
    @Primary
    public LoopModelClient minimalScriptedModelClient() {
        return new LoopModelClient() {

            @Override
            public ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                String userMessage = messages.stream()
                        .filter(m -> "user".equals(m.role()))
                        .reduce((first, second) -> second)
                        .map(ExecutionMessage::content)
                        .orElse("No question provided.");
                return ModelTurnResponse.finalAnswer(
                        "You asked: \"" + userMessage + "\". This is a scripted demo response "
                                + "- swap in a real LoopModelClient for LLM-driven answers.");
            }

            @Override
            public Flux<StreamEvent> stream(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                ModelTurnResponse r = generate(messages, tools);
                return Flux.just(new StreamEvent.Token(r.content()), new StreamEvent.Complete(r.content()));
            }
        };
    }
}
