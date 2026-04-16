package com.example.spring.ai.strands.examples.calcmin;

import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.LoopModelClient;
import com.example.spring.ai.strands.agent.execution.ModelTurnResponse;
import com.example.spring.ai.strands.agent.execution.ToolCallRequest;
import com.example.spring.ai.strands.agent.execution.stream.StreamEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

/**
 * One tool + scripted {@link LoopModelClient} (sqrt of 1764, then final text). Swap for ChatClient in production.
 *
 * @author Vaquar Khan
 */
@Configuration
public class CalculatorMinimalConfiguration {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Bean
    public ToolCallbackProvider calculatorOnly() {
        return () -> new FunctionCallback[] {calculatorTool()};
    }

    static ToolCallback calculatorTool() {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                        .name("calculator")
                        .description("Perfect square root: {\"operation\":\"sqrt\",\"value\":n}")
                        .inputSchema(
                                "{\"type\":\"object\",\"properties\":{\"operation\":{\"type\":\"string\"},\"value\":{\"type\":\"integer\"}}}")
                        .build();
            }

            @Override
            public String call(String arguments) {
                try {
                    JsonNode n = JSON.readTree(arguments);
                    if (!"sqrt".equalsIgnoreCase(n.path("operation").asText())) {
                        return "{\"error\":\"unsupported_operation\"}";
                    }
                    long v = n.path("value").asLong();
                    if (v < 0) {
                        return "{\"error\":\"negative\"}";
                    }
                    double s = Math.sqrt(v);
                    if (s != Math.rint(s)) {
                        return "{\"error\":\"not_perfect_square\"}";
                    }
                    return JSON.writeValueAsString(java.util.Map.of("sqrt", (long) s));
                } catch (Exception e) {
                    return "{\"error\":\"bad_args\"}";
                }
            }
        };
    }

    @Bean
    @Primary
    public LoopModelClient calculatorMinimalScriptedModel() {
        return new LoopModelClient() {
            @Override
            public ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                long toolsSoFar =
                        messages.stream().filter(m -> "tool".equals(m.role())).count();
                if (toolsSoFar == 0) {
                    ObjectNode args = JSON.createObjectNode();
                    args.put("operation", "sqrt");
                    args.put("value", 1764);
                    return ModelTurnResponse.toolCall("calculator", args.toString());
                }
                return ModelTurnResponse.finalAnswer(
                        "The square root of 1764 is 42 (isqrt verified). Scripted demo - use a real LLM for open-ended math.");
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
