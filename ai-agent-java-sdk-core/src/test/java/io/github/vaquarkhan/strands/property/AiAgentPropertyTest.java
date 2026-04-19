package io.github.vaquarkhan.strands.property;

/**
 * @author Vaquar Khan
 */
import io.github.vaquarkhan.strands.AiAgent;
import io.github.vaquarkhan.strands.config.AiAgentProperties;
import io.github.vaquarkhan.strands.execution.ModelTurnResponse;
import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
import io.github.vaquarkhan.strands.execution.AgentExecutionLoop;
import io.github.vaquarkhan.strands.observability.AgentObservability;
import io.github.vaquarkhan.strands.support.MockModelClient;
import io.github.vaquarkhan.strands.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AiAgentPropertyTest {

    @Property(tries = 100)
    void sessionIdentityPropagation(@ForAll("ids") String sessionId, @ForAll("ids") String userId) {
        MockModelClient modelClient = new MockModelClient().addResponse(ModelTurnResponse.finalAnswer("ok"));
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 2, "agent");
        AiAgentProperties properties = new AiAgentProperties();
        properties.setModelProvider("openai");
        properties.setModelId("gpt");
        properties.setSystemPrompt("sys");
        AiAgent agent =
                new AiAgent(
                        loop,
                        new ToolRegistry(new LinkedHashMap<>(), properties.getSecurity()),
                        properties,
                        List.of(),
                        new AgentObservability(
                                new SimpleMeterRegistry(), ObservationRegistry.NOOP, properties.getSecurity()));
        var response = agent.execute("hello", new AgentExecutionContext(sessionId, userId, Map.of("x", "y")));
        assertEquals(sessionId, response.reasoningTrace().sessionId());
    }

    @net.jqwik.api.Provide
    net.jqwik.api.Arbitrary<String> ids() {
        return Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20);
    }
}
