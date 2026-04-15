package com.example.spring.ai.strands.examples.toolfilter;

import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.LoopModelClient;
import com.example.spring.ai.strands.agent.execution.ModelTurnResponse;
import com.example.spring.ai.strands.agent.execution.ToolCallRequest;
import com.example.spring.ai.strands.agent.execution.stream.StreamEvent;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

/**
 * Scripted model: only {@code demo_calc} should be invokable after YAML filtering (see README).
 *
 * @author Vaquar Khan
 */
@Configuration
public class ToolFilterConfiguration {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Bean
    @Primary
    public LoopModelClient toolFilterScriptedModel() {
        return new LoopModelClient() {
            @Override
            public ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                long prior =
                        messages.stream().filter(m -> "tool".equals(m.role())).count();
                if (prior == 0) {
                    ObjectNode args = JSON.createObjectNode();
                    args.put("a", 6);
                    args.put("b", 7);
                    return ModelTurnResponse.toolCall("demo_calc", args.toString());
                }
                return ModelTurnResponse.finalAnswer(
                        "Filtered tools: only demo_calc and public_echo are allowlisted; admin_secret and alpha_nav are"
                                + " removed. Scripted demo completed after demo_calc(6,7)=42.");
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
