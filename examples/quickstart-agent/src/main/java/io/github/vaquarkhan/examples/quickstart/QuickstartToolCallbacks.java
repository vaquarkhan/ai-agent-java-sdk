package io.github.vaquarkhan.examples.quickstart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Tools analogous to {@code strands_tools} {@code calculator} and {@code current_time}, plus a
 * custom {@code letter_counter} matching the quickstart pattern.
 *
 * @author Vaquar Khan
 */
public final class QuickstartToolCallbacks {

    private static final ObjectMapper JSON = new ObjectMapper();

    private QuickstartToolCallbacks() {}

    public static ToolCallback calculator() {
        return new ToolCallback() {
            @Override
            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                        .name("calculator")
                        .description("Divide two positive integers. JSON: {\"dividend\":int,\"divisor\":int}")
                        .inputSchema(
                                """
                                {"type":"object","properties":{"dividend":{"type":"integer"},"divisor":{"type":"integer"}},"required":["dividend","divisor"]}
                                """)
                        .build();
            }

            @Override
            public String call(String arguments) {
                try {
                    JsonNode n = JSON.readTree(arguments);
                    long a = n.path("dividend").asLong();
                    long b = n.path("divisor").asLong();
                    if (b == 0) {
                        return "{\"error\":\"division_by_zero\"}";
                    }
                    return JSON.writeValueAsString(java.util.Map.of("quotient", a / b, "remainder", a % b));
                } catch (Exception e) {
                    return "{\"error\":\"invalid_arguments\"}";
                }
            }
        };
    }

    public static ToolCallback currentTime() {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                        .name("current_time")
                        .description("Returns the current UTC time as an ISO-8601 instant.")
                        .inputSchema("{\"type\":\"object\"}")
                        .build();
            }

            @Override
            public String call(String arguments) {
                ObjectNode node = JSON.createObjectNode();
                node.put("utc", Instant.now().toString());
                return node.toString();
            }
        };
    }

    public static ToolCallback letterCounter() {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                        .name("letter_counter")
                        .description(
                                "Count occurrences of a letter in a word. JSON: {\"word\":string,\"letter\":string}")
                        .inputSchema(
                                """
                                {"type":"object","properties":{"word":{"type":"string"},"letter":{"type":"string"}},"required":["word","letter"]}
                                """)
                        .build();
            }

            @Override
            public String call(String arguments) {
                try {
                    JsonNode n = JSON.readTree(arguments);
                    String word = n.path("word").asText("");
                    String letter = n.path("letter").asText("");
                    if (letter.length() != 1) {
                        return "{\"error\":\"letter_must_be_single_character\"}";
                    }
                    int count = 0;
                    char c = Character.toLowerCase(letter.charAt(0));
                    for (char ch : word.toCharArray()) {
                        if (Character.toLowerCase(ch) == c) {
                            count++;
                        }
                    }
                    return JSON.writeValueAsString(java.util.Map.of("count", count));
                } catch (Exception e) {
                    return "{\"error\":\"invalid_arguments\"}";
                }
            }
        };
    }
}
