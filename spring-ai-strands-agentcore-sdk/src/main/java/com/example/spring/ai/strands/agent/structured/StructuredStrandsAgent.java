package com.example.spring.ai.strands.agent.structured;

import com.example.spring.ai.strands.agent.StrandsAgent;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;

/**
 * Thin wrapper around {@link StrandsAgent} that requests JSON-only output and parses it into a target type.
 *
 * <p>This is a provider-agnostic adapter: it works with any Spring AI provider because it relies on prompt
 * steering plus robust JSON extraction/parsing on the client side.
 *
 * @author Vaquar Khan
 */
public class StructuredStrandsAgent {

    private final StrandsAgent delegate;
    private final ObjectMapper objectMapper;

    public StructuredStrandsAgent(StrandsAgent delegate, ObjectMapper objectMapper) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public <T> T executeForObject(String userPrompt, Class<T> targetType) {
        return executeForObject(userPrompt, targetType, new StrandsExecutionContext(null, null, Map.of()));
    }

    public <T> T executeForObject(String userPrompt, Class<T> targetType, StrandsExecutionContext context) {
        Objects.requireNonNull(userPrompt, "userPrompt");
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(context, "context");

        String prompt = userPrompt
                + "\n\n"
                + "Return ONLY valid JSON (no markdown, no prose). "
                + "The JSON must deserialize into: " + targetType.getName() + ".";

        String responseText = delegate.execute(prompt, context).content();
        String json = JsonBlockExtractor.extractFirstJsonBlock(responseText);

        try {
            return objectMapper.readValue(json, targetType);
        } catch (Exception e) {
            throw new StructuredOutputParseException(
                    "Failed to parse JSON into " + targetType.getName() + ". Extracted JSON: " + json, e);
        }
    }
}

