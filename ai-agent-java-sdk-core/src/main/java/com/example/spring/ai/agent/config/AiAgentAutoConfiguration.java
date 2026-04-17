package com.example.spring.ai.agent.config;

import com.example.spring.ai.agent.AiAgent;
import com.example.spring.ai.agent.approval.ApprovalManager;
import com.example.spring.ai.agent.api.Advisor;
import com.example.spring.ai.agent.execution.ChatModelLoopModelClient;
import com.example.spring.ai.agent.execution.LoopModelClient;
import com.example.spring.ai.agent.execution.NoopLoopModelClient;
import com.example.spring.ai.agent.execution.AgentExecutionLoop;
import com.example.spring.ai.agent.observability.AgentObservability;
import com.example.spring.ai.agent.tool.ToolBridge;
import com.example.spring.ai.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Vaquar Khan
 */
@AutoConfiguration
@ConditionalOnProperty(name = "ai.agent.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AiAgentProperties.class)
public class AiAgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ToolRegistry aiAgentToolRegistry(
            List<ToolCallbackProvider> providers, AiAgentProperties properties) {
        AiAgentPropertiesValidator.validateOrThrow(properties);
        return ToolBridge.discoverTools(providers, properties);
    }

    @Bean
    @ConditionalOnBean(ChatModel.class)
    @ConditionalOnMissingBean(LoopModelClient.class)
    public LoopModelClient aiAgentChatModelLoopModelClient(ChatModel chatModel) {
        return new ChatModelLoopModelClient(chatModel);
    }

    @Bean
    @ConditionalOnMissingBean(LoopModelClient.class)
    public LoopModelClient aiAgentNoopLoopModelClient() {
        return new NoopLoopModelClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentExecutionLoop agentExecutionLoop(
            LoopModelClient modelClient,
            AiAgentProperties properties,
            ObjectProvider<ApprovalManager> approvalManagerProvider) {
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, properties.getMaxIterations(), "ai-agent");
        loop.setApprovalManager(approvalManagerProvider.getIfAvailable());
        return loop;
    }

    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    public MeterRegistry aiAgentMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(ObservationRegistry.class)
    public ObservationRegistry aiAgentObservationRegistry() {
        return ObservationRegistry.NOOP;
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentObservability agentObservability(
            MeterRegistry meterRegistry,
            ObservationRegistry observationRegistry,
            AiAgentProperties properties) {
        return new AgentObservability(meterRegistry, observationRegistry, properties.getSecurity());
    }

    @Bean
    @ConditionalOnMissingBean
    public AiAgent aiAgent(
            AgentExecutionLoop executionLoop,
            ToolRegistry toolRegistry,
            AiAgentProperties properties,
            List<Advisor> advisors,
            AgentObservability observability,
            ObjectProvider<ApprovalManager> approvalManagerProvider) {
        AiAgent agent = new AiAgent(executionLoop, toolRegistry, properties, advisors, observability);
        ApprovalManager approvalManager = approvalManagerProvider.getIfAvailable();
        if (approvalManager != null) {
            agent.setApprovalManager(approvalManager);
        }
        return agent;
    }
}
