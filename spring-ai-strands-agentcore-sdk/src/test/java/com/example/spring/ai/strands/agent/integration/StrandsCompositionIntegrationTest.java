package com.example.spring.ai.strands.agent.integration;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.StrandsAgent;
import com.example.spring.ai.strands.agent.api.Advisor;
import com.example.spring.ai.strands.agent.config.StrandsAgentAutoConfiguration;
import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.LoopModelClient;
import com.example.spring.ai.strands.agent.execution.ModelTurnResponse;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.execution.stream.StreamEvent;
import com.example.spring.ai.strands.agent.support.MockToolCallbackProvider;
import com.example.spring.ai.strands.agent.support.TestToolCallback;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
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

@SpringBootTest(classes = StrandsCompositionIntegrationTest.TestConfig.class, properties = {
        "strands.agent.model-provider=openai",
        "strands.agent.model-id=gpt",
        "strands.agent.system-prompt=test"
})
@ImportAutoConfiguration(StrandsAgentAutoConfiguration.class)
class StrandsCompositionIntegrationTest {

    @Autowired
    private StrandsAgent strandsAgent;

    @Autowired
    private ToolRegistry toolRegistry;

    @Test
    void runtimeCompositionAndStreamingWorks() {
        assertNotNull(strandsAgent);
        assertTrue(strandsAgent.executeStreaming("hello", StrandsExecutionContext.standalone("s")).collectList().block().size() > 0);
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
