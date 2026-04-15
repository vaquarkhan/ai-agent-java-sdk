package com.example.spring.ai.strands.agent.support;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;

public class StrandsAgentPropertiesBuilder {
    private final StrandsAgentProperties properties = new StrandsAgentProperties();

    public StrandsAgentPropertiesBuilder defaults() {
        properties.setModelProvider("openai");
        properties.setModelId("gpt-test");
        return this;
    }

    public StrandsAgentPropertiesBuilder maxIterations(int maxIterations) {
        properties.setMaxIterations(maxIterations);
        return this;
    }

    public StrandsAgentPropertiesBuilder systemPrompt(String prompt) {
        properties.setSystemPrompt(prompt);
        return this;
    }

    public StrandsAgentProperties build() {
        return properties;
    }
}
