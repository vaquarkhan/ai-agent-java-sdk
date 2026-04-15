package com.example.spring.ai.strands.agent;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.api.Advisor;
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.ModelTurnResponse;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionLoop;
import com.example.spring.ai.strands.agent.observability.StrandsObservability;
import com.example.spring.ai.strands.agent.support.MockModelClient;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StrandsAgentTest {

    @Test
    void executeDelegatesToLoopWithEnrichedPrompt() {
        MockModelClient modelClient = new MockModelClient().addResponse(ModelTurnResponse.finalAnswer("ok"));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        StrandsAgentProperties properties = baseProperties();
        properties.setSystemPrompt("sys");
        Advisor advisor = (messages, context) -> {
            messages.add(new ExecutionMessage("user", "from-advisor"));
            return messages;
        };
        StrandsAgent agent =
                new StrandsAgent(loop, emptyRegistry(properties), properties, List.of(advisor), observability(properties));
        var response = agent.execute("hello", StrandsExecutionContext.standalone("s"));
        assertEquals("ok", response.content());
        assertEquals("from-advisor", modelClient.getCallHistory().get(0).get(2).content());
    }

    @Test
    void systemPromptLoadedFromResource() {
        MockModelClient modelClient = new MockModelClient().addResponse(ModelTurnResponse.finalAnswer("ok"));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        StrandsAgentProperties properties = baseProperties();
        properties.setSystemPromptResource(new DefaultResourceLoader().getResource("classpath:prompts/system.txt"));
        StrandsAgent agent =
                new StrandsAgent(loop, emptyRegistry(properties), properties, List.of(), observability(properties));
        agent.execute("hello", StrandsExecutionContext.standalone("s"));
        assertEquals("resource system prompt", modelClient.getCallHistory().get(0).get(0).content().trim());
    }

    private StrandsAgentProperties baseProperties() {
        StrandsAgentProperties properties = new StrandsAgentProperties();
        properties.setModelProvider("openai");
        properties.setModelId("gpt");
        return properties;
    }

    private static ToolRegistry emptyRegistry(StrandsAgentProperties properties) {
        return new ToolRegistry(new LinkedHashMap<>(), properties.getSecurity());
    }

    private StrandsObservability observability(StrandsAgentProperties properties) {
        return new StrandsObservability(new SimpleMeterRegistry(), ObservationRegistry.NOOP, properties.getSecurity());
    }
}
