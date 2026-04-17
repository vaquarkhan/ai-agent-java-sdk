package com.example.spring.ai.agent.config;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.agent.AiAgent;
import com.example.spring.ai.agent.execution.ExecutionMessage;
import com.example.spring.ai.agent.execution.LoopModelClient;
import com.example.spring.ai.agent.execution.ModelTurnResponse;
import com.example.spring.ai.agent.execution.NoopLoopModelClient;
import com.example.spring.ai.agent.execution.stream.StreamEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Flux;
import static org.assertj.core.api.Assertions.assertThat;

class AiAgentAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class, AiAgentAutoConfiguration.class));

    @Test
    void beansCreatedWhenEnabled() {
        contextRunner
                .withPropertyValues("ai.agent.model-provider=openai", "ai.agent.model-id=gpt")
                .run(context -> assertThat(context).hasSingleBean(AiAgent.class));
    }

    @Test
    void noBeansWhenDisabled() {
        contextRunner
                .withPropertyValues(
                        "ai.agent.enabled=false", "ai.agent.model-provider=openai", "ai.agent.model-id=gpt")
                .run(context -> assertThat(context).doesNotHaveBean(AiAgent.class));
    }

    @Test
    void beansOverridable() {
        contextRunner
                .withBean(LoopModelClient.class, () -> new LoopModelClient() {
                    @Override
                    public ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                        return ModelTurnResponse.finalAnswer("ok");
                    }

                    @Override
                    public Flux<StreamEvent> stream(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                        return Flux.empty();
                    }
                })
                .withPropertyValues("ai.agent.model-provider=openai", "ai.agent.model-id=gpt")
                .run(context -> assertThat(context.getBean(LoopModelClient.class)).isNotInstanceOf(NoopLoopModelClient.class));
    }

    @Test
    void missingRequiredPropertiesFailStartup() {
        contextRunner.run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }
}
