package com.example.spring.ai.strands.agent.property;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import jakarta.validation.Validation;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrandsAgentPropertiesPropertyTest {

    // Feature: spring-ai-strands-agentcore-sdk, Property 1: Configuration round-trip
    @Property(tries = 100)
    void configurationRoundTrip(@ForAll("validProperties") StrandsAgentProperties input) {
        StrandsAgentProperties output = roundTrip(input);
        assertEquals(input.getModelProvider(), output.getModelProvider());
        assertEquals(input.getModelId(), output.getModelId());
        assertEquals(input.getMaxIterations(), output.getMaxIterations());
        assertEquals(input.getToolDiscovery().getIncludePatterns(), output.getToolDiscovery().getIncludePatterns());
        assertEquals(input.getToolDiscovery().getExcludePatterns(), output.getToolDiscovery().getExcludePatterns());
    }

    // Feature: spring-ai-strands-agentcore-sdk, Property 11: Configuration validation rejects invalid values
    @Property(tries = 100)
    void invalidMaxIterationRejected(@ForAll("invalidMaxIterations") int invalidMaxIterations) {
        StrandsAgentProperties properties = new StrandsAgentProperties();
        properties.setModelProvider("openai");
        properties.setModelId("gpt");
        properties.setMaxIterations(invalidMaxIterations);
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertTrue(!validator.validate(properties).isEmpty());
    }

    @Provide
    Arbitrary<StrandsAgentProperties> validProperties() {
        Arbitrary<String> provider = Arbitraries.of("openai", "bedrock", "anthropic");
        Arbitrary<String> model = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20);
        Arbitrary<Integer> max = Arbitraries.integers().between(1, 100);
        Arbitrary<String> glob = Arbitraries.of("*", "calc*", "weather*", "tool-*");
        return net.jqwik.api.Combinators.combine(provider, model, max, glob.list().ofMaxSize(3), glob.list().ofMaxSize(3))
                .as((p, m, mx, includes, excludes) -> {
                    StrandsAgentProperties properties = new StrandsAgentProperties();
                    properties.setModelProvider(p);
                    properties.setModelId(m);
                    properties.setMaxIterations(mx);
                    properties.getToolDiscovery().setIncludePatterns(includes);
                    properties.getToolDiscovery().setExcludePatterns(excludes);
                    return properties;
                });
    }

    @Provide
    Arbitrary<Integer> invalidMaxIterations() {
        return Arbitraries.integers().between(-1000, 0);
    }

    private StrandsAgentProperties roundTrip(StrandsAgentProperties input) {
        StrandsAgentProperties output = new StrandsAgentProperties();
        output.setEnabled(input.isEnabled());
        output.setModelProvider(input.getModelProvider());
        output.setModelId(input.getModelId());
        output.setSystemPrompt(input.getSystemPrompt());
        output.setSystemPromptResource(input.getSystemPromptResource());
        output.setMaxIterations(input.getMaxIterations());
        output.getToolDiscovery().setEnabled(input.getToolDiscovery().isEnabled());
        output.getToolDiscovery().setIncludePatterns(input.getToolDiscovery().getIncludePatterns());
        output.getToolDiscovery().setExcludePatterns(input.getToolDiscovery().getExcludePatterns());
        return output;
    }
}
