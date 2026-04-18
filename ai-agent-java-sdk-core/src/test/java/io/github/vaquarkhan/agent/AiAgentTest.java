package io.github.vaquarkhan.agent;

/**
 * @author Vaquar Khan
 */
import io.github.vaquarkhan.agent.api.Advisor;
import io.github.vaquarkhan.agent.config.AiAgentProperties;
import io.github.vaquarkhan.agent.execution.ExecutionMessage;
import io.github.vaquarkhan.agent.execution.ModelTurnResponse;
import io.github.vaquarkhan.agent.execution.AgentExecutionContext;
import io.github.vaquarkhan.agent.execution.AgentExecutionLoop;
import io.github.vaquarkhan.agent.observability.AgentObservability;
import io.github.vaquarkhan.agent.support.MockModelClient;
import io.github.vaquarkhan.agent.tool.ToolRegistry;
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
