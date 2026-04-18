package io.github.vaquarkhan.agent.config;

import io.github.vaquarkhan.agent.AiAgent;
import io.github.vaquarkhan.agent.approval.ApprovalManager;
import io.github.vaquarkhan.agent.api.Advisor;
import io.github.vaquarkhan.agent.execution.ChatModelLoopModelClient;
import io.github.vaquarkhan.agent.execution.DeferredChatModelLoopModelClient;
import io.github.vaquarkhan.agent.execution.LoopModelClient;
import io.github.vaquarkhan.agent.execution.AgentExecutionLoop;
import io.github.vaquarkhan.agent.observability.AgentObservability;
import io.github.vaquarkhan.agent.tool.ToolBridge;
import io.github.vaquarkhan.agent.tool.ToolRegistry;
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
     * Wires {@link LoopModelClient} from Spring AI's {@link ChatModel} without
     * {@code @ConditionalOnBean(ChatModel.class)} (that condition is evaluated during auto-config
     * registration and can run before another auto-config has registered {@code ChatModel}).
     * <p>When a {@code ChatModel} is already resolvable at bean-creation time, a
     * {@link ChatModelLoopModelClient} is used directly. Otherwise a {@link DeferredChatModelLoopModelClient}
     * resolves the provider on first use (after the context graph is complete).
     * <p>If multiple {@link ChatModel} beans exist, the first candidate is used; prefer {@code @Primary} on one bean.
     */
    @Bean
    @ConditionalOnMissingBean(LoopModelClient.class)
    public LoopModelClient aiAgentLoopModelClient(ObjectProvider<ChatModel> chatModel) {
        Optional<ChatModel> first = chatModel.stream().findFirst();
        return first.<LoopModelClient>map(ChatModelLoopModelClient::new)
                .orElseGet(() -> new DeferredChatModelLoopModelClient(chatModel));
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
