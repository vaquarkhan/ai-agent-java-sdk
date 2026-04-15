package com.example.spring.ai.strands.agent.property;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.StrandsAgent;
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
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
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StrandsAgentPropertyTest {

    @Property(tries = 100)
    void sessionIdentityPropagation(@ForAll("ids") String sessionId, @ForAll("ids") String userId) {
        MockModelClient modelClient = new MockModelClient().addResponse(ModelTurnResponse.finalAnswer("ok"));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 2, "agent");
        StrandsAgentProperties properties = new StrandsAgentProperties();
        properties.setModelProvider("openai");
        properties.setModelId("gpt");
        properties.setSystemPrompt("sys");
        StrandsAgent agent =
                new StrandsAgent(
                        loop,
                        new ToolRegistry(new LinkedHashMap<>(), properties.getSecurity()),
                        properties,
                        List.of(),
                        new StrandsObservability(
                                new SimpleMeterRegistry(), ObservationRegistry.NOOP, properties.getSecurity()));
        var response = agent.execute("hello", new StrandsExecutionContext(sessionId, userId, Map.of("x", "y")));
        assertEquals(sessionId, response.reasoningTrace().sessionId());
    }

    @net.jqwik.api.Provide
    net.jqwik.api.Arbitrary<String> ids() {
        return Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20);
    }
}
