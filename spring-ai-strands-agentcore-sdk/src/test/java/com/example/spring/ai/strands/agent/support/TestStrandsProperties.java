package com.example.spring.ai.strands.agent.support;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;

/** Minimal valid {@link StrandsAgentProperties} for tests. */
public final class TestStrandsProperties {

    private TestStrandsProperties() {}

    public static StrandsAgentProperties minimal() {
        StrandsAgentProperties p = new StrandsAgentProperties();
        p.setModelProvider("openai");
        p.setModelId("gpt-test");
        return p;
    }

    public static StrandsAgentProperties withToolDiscovery(StrandsAgentProperties.ToolDiscovery discovery) {
        StrandsAgentProperties p = minimal();
        p.setToolDiscovery(discovery);
        return p;
    }
}
