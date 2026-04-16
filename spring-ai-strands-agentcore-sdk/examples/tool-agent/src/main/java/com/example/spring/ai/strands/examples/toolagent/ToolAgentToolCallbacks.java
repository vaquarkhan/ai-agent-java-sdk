package com.example.spring.ai.strands.examples.toolagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Custom tool callbacks for the tool-agent example: weather lookup and unit converter.
 *
 * @author Vaquar Khan
 */
public final class ToolAgentToolCallbacks {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ToolAgentToolCallbacks() {}

    /**
     * Simulated weather lookup tool. Returns canned weather data for any city.
     */
    public static ToolCallback weatherLookup() {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                        .name("weather_lookup")
                        .description("Look up current weather for a city. JSON: {\"city\":string}")
                        .inputSchema(
                                "{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}},\"required\":[\"city\"]}")
                        .build();
            }

            @Override
            public String call(String arguments) {
                try {
                    JsonNode n = JSON.readTree(arguments);
                    String city = n.path("city").asText("unknown");
                    // Simulated weather data
                    return JSON.writeValueAsString(Map.of(
                            "city", city,
                            "temperature_celsius", 22,
                            "condition", "partly cloudy",
                            "humidity_percent", 65));
                } catch (Exception e) {
                    return "{\"error\":\"invalid_arguments\"}";
                }
            }
        };
    }

    /**
     * Unit converter tool. Converts between Celsius/Fahrenheit and km/miles.
     */
    public static ToolCallback unitConverter() {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                        .name("unit_converter")
                        .description("Convert units. JSON: {\"value\":number,\"from\":string,\"to\":string}")
                        .inputSchema(
                                "{\"type\":\"object\",\"properties\":{\"value\":{\"type\":\"number\"},\"from\":{\"type\":\"string\"},\"to\":{\"type\":\"string\"}},\"required\":[\"value\",\"from\",\"to\"]}")
                        .build();
            }

            @Override
            public String call(String arguments) {
                try {
                    JsonNode n = JSON.readTree(arguments);
                    double value = n.path("value").asDouble();
                    String from = n.path("from").asText("").toLowerCase();
                    String to = n.path("to").asText("").toLowerCase();
                    double result;
                    if ("celsius".equals(from) && "fahrenheit".equals(to)) {
                        result = value * 9.0 / 5.0 + 32;
                    } else if ("fahrenheit".equals(from) && "celsius".equals(to)) {
                        result = (value - 32) * 5.0 / 9.0;
                    } else if ("km".equals(from) && "miles".equals(to)) {
                        result = value * 0.621371;
                    } else if ("miles".equals(from) && "km".equals(to)) {
                        result = value / 0.621371;
                    } else {
                        return "{\"error\":\"unsupported_conversion\"}";
                    }
                    return JSON.writeValueAsString(Map.of(
                            "value", Math.round(result * 100.0) / 100.0,
                            "from", from,
                            "to", to));
                } catch (Exception e) {
                    return "{\"error\":\"invalid_arguments\"}";
                }
            }
        };
    }
}
