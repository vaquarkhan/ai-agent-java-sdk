package io.github.vaquarkhan.strands.examples.toolagent;

import io.github.vaquarkhan.strands.execution.ExecutionMessage;
import io.github.vaquarkhan.strands.execution.LoopModelClient;
import io.github.vaquarkhan.strands.execution.ModelTurnResponse;
import io.github.vaquarkhan.strands.execution.ToolCallRequest;
import io.github.vaquarkhan.strands.execution.stream.StreamEvent;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

/**
 * Wires custom tools and a scripted {@link LoopModelClient} for the tool-agent demo.
 *
 * The scripted model calls weather_lookup, then unit_converter, then returns a summary.
 * Swap this bean with a ChatClient-backed implementation for real LLM behavior.
 *
 * @author Vaquar Khan
 */
@Configuration
public class ToolAgentConfiguration {

    @Bean
    public ToolCallbackProvider toolAgentCallbackProvider() {
        return () -> new ToolCallback[] {
            ToolAgentToolCallbacks.weatherLookup(),
            ToolAgentToolCallbacks.unitConverter()
        };
    }

    @Bean
    @Primary
    public LoopModelClient toolAgentScriptedModelClient() {
        return new LoopModelClient() {

            @Override
            public ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                long priorToolResults =
                        messages.stream().filter(m -> "tool".equals(m.role())).count();
                return switch ((int) priorToolResults) {
                    case 0 -> ModelTurnResponse.toolCall(
                            "weather_lookup", "{\"city\":\"Seattle\"}");
                    case 1 -> ModelTurnResponse.toolCall(
                            "unit_converter",
                            "{\"value\":22,\"from\":\"celsius\",\"to\":\"fahrenheit\"}");
                    default -> ModelTurnResponse.finalAnswer(
                            "Seattle is currently 22C (71.6F), partly cloudy with 65% humidity. "
                                    + "(Scripted demo - swap in a real LoopModelClient for LLM-driven behavior.)");
                };
            }

            @Override
            public Flux<StreamEvent> stream(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                ModelTurnResponse r = generate(messages, tools);
                if (r.hasToolCall()) {
                    ToolCallRequest req = r.toolCallRequest();
                    return Flux.just(new StreamEvent.ToolCallBoundary(req), new StreamEvent.Complete(""));
                }
                String c = r.content();
                return Flux.just(new StreamEvent.Token(c), new StreamEvent.Complete(c));
            }
        };
    }
}
