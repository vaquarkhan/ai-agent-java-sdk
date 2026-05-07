package io.github.vaquarkhan.examples.quickstart;

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
 * Wires tools and a {@link LoopModelClient} for the quickstart demo.
 *
 * <p>The scripted model replays a tool sequence so the example runs without cloud API keys.
 * Replace this bean with a {@link LoopModelClient} backed by Spring AI {@code ChatClient}
 * when you want a real foundation model.
 *
 * @author Vaquar Khan
 */
@Configuration
public class QuickstartConfiguration {

    @Bean
    public ToolCallbackProvider quickstartToolCallbackProvider() {
        return () -> new ToolCallback[] {
            QuickstartToolCallbacks.calculator(),
            QuickstartToolCallbacks.currentTime(),
            QuickstartToolCallbacks.letterCounter()
        };
    }

    /**
     * Drives the same three tool calls as a typical successful run on the default
     * prompt, then returns a short final summary (no LLM).
     */
    @Bean
    @Primary
    public LoopModelClient quickstartScriptedModelClient() {
        return new LoopModelClient() {

            @Override
            public ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                long priorToolResults =
                        messages.stream().filter(m -> "tool".equals(m.role())).count();
                return switch ((int) priorToolResults) {
                    case 0 -> ModelTurnResponse.toolCall("current_time", "{}");
                    case 1 -> ModelTurnResponse.toolCall(
                            "calculator", "{\"dividend\":3111696,\"divisor\":74088}");
                    case 2 -> ModelTurnResponse.toolCall(
                            "letter_counter", "{\"word\":\"strawberry\",\"letter\":\"r\"}");
                    default -> ModelTurnResponse.finalAnswer(
                            "Summary: retrieved current UTC time, divided 3111696/74088, counted "
                                    + "letter 'r' in \"strawberry\". (Scripted demo model - swap in "
                                    + "ChatClient-backed LoopModelClient for a real LLM.)");
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
