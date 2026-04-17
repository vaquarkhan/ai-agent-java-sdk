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
import java.util.Optional;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Vaquar Khan
 */
@AutoConfiguration
@AutoConfigureAfter(
        name = {
            // Spring AI 1.x: run after model providers register ChatModel (string names avoid compile deps).
            "org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
            "org.springframework.ai.model.bedrock.converse.autoconfigure.BedrockConverseProxyChatAutoConfiguration",
            "org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration",
            "org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiChatAutoConfiguration"
        })
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

    /**
     * Resolves {@link ChatModel} via {@link org.springframework.beans.factory.ObjectProvider} at bean-creation time
     * (not via {@code @ConditionalOnBean(ChatModel.class)}, which can run before provider auto-config registers
     * {@code ChatModel}, causing {@link NoopLoopModelClient} to win).
     * <p>If multiple {@link ChatModel} beans exist, the first candidate is used; prefer {@code @Primary} on one bean.
     */
    @Bean
    @ConditionalOnMissingBean(LoopModelClient.class)
    public LoopModelClient aiAgentLoopModelClient(ObjectProvider<ChatModel> chatModel) {
        Optional<ChatModel> first = chatModel.stream().findFirst();
        return first.<LoopModelClient>map(ChatModelLoopModelClient::new).orElseGet(NoopLoopModelClient::new);
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
