package com.example.spring.ai.agent.support;

import com.example.spring.ai.agent.config.AiAgentProperties;

/**
 * Minimal valid {@link AiAgentProperties} for tests.
 *
 * @author Vaquar Khan
 */
public final class TestAiAgentProperties {

    private TestAiAgentProperties() {}

    public static AiAgentProperties minimal() {
        AiAgentProperties p = new AiAgentProperties();
        p.setModelProvider("openai");
        p.setModelId("gpt-test");
        return p;
    }

    public static AiAgentProperties withToolDiscovery(AiAgentProperties.ToolDiscovery discovery) {
        AiAgentProperties p = minimal();
        p.setToolDiscovery(discovery);
        return p;
    }
}
