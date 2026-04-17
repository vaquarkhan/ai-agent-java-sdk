package com.example.spring.ai.agent;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.agent.api.Advisor;
import com.example.spring.ai.agent.config.AiAgentProperties;
import com.example.spring.ai.agent.execution.ExecutionMessage;
import com.example.spring.ai.agent.execution.ModelTurnResponse;
import com.example.spring.ai.agent.execution.AgentExecutionContext;
import com.example.spring.ai.agent.execution.AgentExecutionLoop;
import com.example.spring.ai.agent.observability.AgentObservability;
import com.example.spring.ai.agent.support.MockModelClient;
import com.example.spring.ai.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AiAgentTest {

    @Test
    void executeDelegatesToLoopWithEnrichedPrompt() {
        MockModelClient modelClient = new MockModelClient().addResponse(ModelTurnResponse.finalAnswer("ok"));
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        AiAgentProperties properties = baseProperties();
        properties.setSystemPrompt("sys");
        Advisor advisor = (messages, context) -> {
            messages.add(new ExecutionMessage("user", "from-advisor"));
            return messages;
        };
        AiAgent agent =
                new AiAgent(loop, emptyRegistry(properties), properties, List.of(advisor), observability(properties));
        var response = agent.execute("hello", AgentExecutionContext.standalone("s"));
        assertEquals("ok", response.content());
        assertEquals("from-advisor", modelClient.getCallHistory().get(0).get(2).content());
    }

    @Test
    void systemPromptLoadedFromResource() {
        MockModelClient modelClient = new MockModelClient().addResponse(ModelTurnResponse.finalAnswer("ok"));
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        AiAgentProperties properties = baseProperties();
        properties.setSystemPromptResource(new DefaultResourceLoader().getResource("classpath:prompts/system.txt"));
        AiAgent agent =
                new AiAgent(loop, emptyRegistry(properties), properties, List.of(), observability(properties));
        agent.execute("hello", AgentExecutionContext.standalone("s"));
        assertEquals("resource system prompt", modelClient.getCallHistory().get(0).get(0).content().trim());
    }

    private AiAgentProperties baseProperties() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.setModelProvider("openai");
        properties.setModelId("gpt");
        return properties;
    }

    private static ToolRegistry emptyRegistry(AiAgentProperties properties) {
        return new ToolRegistry(new LinkedHashMap<>(), properties.getSecurity());
    }

    private AgentObservability observability(AiAgentProperties properties) {
        return new AgentObservability(new SimpleMeterRegistry(), ObservationRegistry.NOOP, properties.getSecurity());
    }
}
