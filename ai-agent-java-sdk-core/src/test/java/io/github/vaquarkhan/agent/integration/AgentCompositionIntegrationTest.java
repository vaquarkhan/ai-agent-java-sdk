package io.github.vaquarkhan.agent.integration;

/**
 * @author Vaquar Khan
 */
import io.github.vaquarkhan.agent.AiAgent;
import io.github.vaquarkhan.agent.api.Advisor;
import io.github.vaquarkhan.agent.config.AiAgentAutoConfiguration;
import io.github.vaquarkhan.agent.execution.ExecutionMessage;
import io.github.vaquarkhan.agent.execution.LoopModelClient;
import io.github.vaquarkhan.agent.execution.ModelTurnResponse;
import io.github.vaquarkhan.agent.execution.AgentExecutionContext;
import io.github.vaquarkhan.agent.execution.stream.StreamEvent;
import io.github.vaquarkhan.agent.support.MockToolCallbackProvider;
import io.github.vaquarkhan.agent.support.TestToolCallback;
import io.github.vaquarkhan.agent.tool.ToolRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = AgentCompositionIntegrationTest.TestConfig.class, properties = {
        "ai.agent.model-provider=openai",
        "ai.agent.model-id=gpt",
        "ai.agent.system-prompt=test"
})
@ImportAutoConfiguration(AiAgentAutoConfiguration.class)
class AgentCompositionIntegrationTest {

    @Autowired
    private AiAgent aiAgent;

    @Autowired
    private ToolRegistry toolRegistry;

    @Test
    void runtimeCompositionAndStreamingWorks() {
        assertNotNull(aiAgent);
        assertTrue(aiAgent.executeStreaming("hello", AgentExecutionContext.standalone("s")).collectList().block().size() > 0);
    }

    @Test
    void toolProvidersDiscovered() {
        assertTrue(toolRegistry.hasTool("browser_search"));
        assertTrue(toolRegistry.hasTool("code_exec"));
        assertTrue(toolRegistry.hasTool("mcp_lookup"));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        LoopModelClient loopModelClient() {
            return new LoopModelClient() {
                @Override
                public ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                    return ModelTurnResponse.finalAnswer("ok");
                }

                @Override
                public Flux<StreamEvent> stream(List<ExecutionMessage> messages, List<ToolCallback> tools) {
                    return Flux.just(new StreamEvent.Token("ok"), new StreamEvent.Complete("ok"));
                }
            };
        }

        @Bean
        Advisor memoryAdvisor() {
            return (messages, context) -> {
                ArrayList<ExecutionMessage> copy = new ArrayList<>(messages);
                copy.add(new ExecutionMessage("user", "memory-enrichment"));
                return copy;
            };
        }

        @Bean
        ToolCallbackProvider browserToolCallbackProvider() {
            return new MockToolCallbackProvider(List.of(new TestToolCallback("browser_search", "browser", a -> "ok")));
        }

        @Bean
        ToolCallbackProvider codeInterpreterToolCallbackProvider() {
            return new MockToolCallbackProvider(List.of(new TestToolCallback("code_exec", "code", a -> "ok")));
        }

        @Bean
        ToolCallbackProvider mcpToolCallbackProvider() {
            return new MockToolCallbackProvider(List.of(new TestToolCallback("mcp_lookup", "mcp", a -> "ok")));
        }
    }
}
