package io.github.vaquarkhan.agent.config;

/**
 * @author Vaquar Khan
 */
import io.github.vaquarkhan.agent.AiAgent;
import io.github.vaquarkhan.agent.execution.ExecutionMessage;
import io.github.vaquarkhan.agent.execution.LoopModelClient;
import io.github.vaquarkhan.agent.execution.ModelTurnResponse;
import io.github.vaquarkhan.agent.execution.ChatModelLoopModelClient;
import io.github.vaquarkhan.agent.execution.NoopLoopModelClient;
import io.github.vaquarkhan.agent.execution.stream.StreamEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Flux;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
    void chatModelBeanYieldsChatModelLoopModelClient() {
        ChatModel stubModel = mock(ChatModel.class);
        contextRunner
                .withBean(ChatModel.class, () -> stubModel)
                .withPropertyValues("ai.agent.model-provider=openai", "ai.agent.model-id=gpt")
                .run(context ->
                        assertThat(context.getBean(LoopModelClient.class)).isInstanceOf(ChatModelLoopModelClient.class));
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
