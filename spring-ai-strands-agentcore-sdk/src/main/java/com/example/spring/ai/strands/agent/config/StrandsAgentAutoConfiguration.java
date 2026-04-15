package com.example.spring.ai.strands.agent.config;

import com.example.spring.ai.strands.agent.StrandsAgent;
import com.example.spring.ai.strands.agent.api.Advisor;
import com.example.spring.ai.strands.agent.execution.LoopModelClient;
import com.example.spring.ai.strands.agent.execution.NoopLoopModelClient;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionLoop;
import com.example.spring.ai.strands.agent.observability.StrandsObservability;
import com.example.spring.ai.strands.agent.tool.ToolBridge;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author Vaquar Khan
 */
@AutoConfiguration
@ConditionalOnProperty(name = "strands.agent.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(StrandsAgentProperties.class)
public class StrandsAgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ToolRegistry strandsToolRegistry(
            List<ToolCallbackProvider> providers, StrandsAgentProperties properties) {
        StrandsAgentPropertiesValidator.validateOrThrow(properties);
        return ToolBridge.discoverTools(providers, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public LoopModelClient strandsLoopModelClient() {
        return new NoopLoopModelClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public StrandsExecutionLoop strandsExecutionLoop(LoopModelClient modelClient, StrandsAgentProperties properties) {
        return new StrandsExecutionLoop(modelClient, properties.getMaxIterations(), "strands-agent");
    }

    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    public MeterRegistry strandsMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(ObservationRegistry.class)
    public ObservationRegistry strandsObservationRegistry() {
        return ObservationRegistry.NOOP;
    }

    @Bean
    @ConditionalOnMissingBean
    public StrandsObservability strandsObservability(
            MeterRegistry meterRegistry,
            ObservationRegistry observationRegistry,
            StrandsAgentProperties properties) {
        return new StrandsObservability(meterRegistry, observationRegistry, properties.getSecurity());
    }

    @Bean
    @ConditionalOnMissingBean
    public StrandsAgent strandsAgent(
            StrandsExecutionLoop executionLoop,
            ToolRegistry toolRegistry,
            StrandsAgentProperties properties,
            List<Advisor> advisors,
            StrandsObservability observability) {
        return new StrandsAgent(executionLoop, toolRegistry, properties, advisors, observability);
    }
}
