package io.github.vaquarkhan.agent.support;

/**
 * @author Vaquar Khan
 */
import io.github.vaquarkhan.agent.config.AiAgentProperties;

public class AiAgentPropertiesBuilder {
    private final AiAgentProperties properties = new AiAgentProperties();

    public AiAgentPropertiesBuilder defaults() {
        properties.setModelProvider("openai");
        properties.setModelId("gpt-test");
        return this;
    }

    public AiAgentPropertiesBuilder maxIterations(int maxIterations) {
        properties.setMaxIterations(maxIterations);
        return this;
    }

    public AiAgentPropertiesBuilder systemPrompt(String prompt) {
        properties.setSystemPrompt(prompt);
        return this;
    }

    public AiAgentProperties build() {
        return properties;
    }
}
