package com.example.spring.ai.strands.agent.session;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.ModelTurnResponse;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionLoop;
import com.example.spring.ai.strands.agent.model.StrandsAgentResponse;
import com.example.spring.ai.strands.agent.observability.StrandsObservability;
import com.example.spring.ai.strands.agent.StrandsAgent;
import com.example.spring.ai.strands.agent.support.MockModelClient;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionManagerTest {

    /**
     * Simple in-memory session manager for testing.
     */
    static class InMemorySessionManager implements SessionManager {
        private final Map<String, List<ExecutionMessage>> store = new HashMap<>();

        @Override
        public void save(String sessionId, List<ExecutionMessage> messages) {
            store.put(sessionId, new ArrayList<>(messages));
        }

        @Override
        public List<ExecutionMessage> load(String sessionId) {
            return store.getOrDefault(sessionId, List.of());
        }

        @Override
        public void delete(String sessionId) {
            store.remove(sessionId);
        }

        @Override
        public boolean exists(String sessionId) {
            return store.containsKey(sessionId);
        }
    }

    @Test
    void sessionMessagesLoadedBeforeExecution() {
        InMemorySessionManager sessionManager = new InMemorySessionManager();
        sessionManager.save("session-1", List.of(
                new ExecutionMessage("user", "previous question"),
                new ExecutionMessage("assistant", "previous answer")));

        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.finalAnswer("new answer"));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        StrandsAgent agent = createAgent(loop, modelClient, sessionManager);

        agent.execute("new question", StrandsExecutionContext.standalone("session-1"));

        // Model should receive: system + session messages + new user message
        List<ExecutionMessage> sentToModel = modelClient.getCallHistory().get(0);
        assertEquals("system", sentToModel.get(0).role());
        assertEquals("previous question", sentToModel.get(1).content());
        assertEquals("previous answer", sentToModel.get(2).content());
        assertEquals("new question", sentToModel.get(3).content());
    }

    @Test
    void sessionMessagesSavedAfterExecution() {
        InMemorySessionManager sessionManager = new InMemorySessionManager();

        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.finalAnswer("the answer"));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        StrandsAgent agent = createAgent(loop, modelClient, sessionManager);

        agent.execute("the question", StrandsExecutionContext.standalone("session-2"));

        assertTrue(sessionManager.exists("session-2"));
        List<ExecutionMessage> saved = sessionManager.load("session-2");
        // Should contain: user message + assistant response
        assertEquals(2, saved.size());
        assertEquals("user", saved.get(0).role());
        assertEquals("the question", saved.get(0).content());
        assertEquals("assistant", saved.get(1).role());
        assertEquals("the answer", saved.get(1).content());
    }

    @Test
    void withoutSessionManagerExecutionWorksNormally() {
        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.finalAnswer("ok"));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        StrandsAgent agent = createAgent(loop, modelClient, null);

        StrandsAgentResponse response = agent.execute("hello", StrandsExecutionContext.standalone("s"));
        assertEquals("ok", response.content());
    }

    @Test
    void sessionExistsAndDeleteOperations() {
        InMemorySessionManager sessionManager = new InMemorySessionManager();
        assertFalse(sessionManager.exists("s1"));

        sessionManager.save("s1", List.of(new ExecutionMessage("user", "hi")));
        assertTrue(sessionManager.exists("s1"));

        sessionManager.delete("s1");
        assertFalse(sessionManager.exists("s1"));
        assertTrue(sessionManager.load("s1").isEmpty());
    }

    private StrandsAgent createAgent(StrandsExecutionLoop loop, MockModelClient modelClient,
                                     SessionManager sessionManager) {
        StrandsAgentProperties properties = new StrandsAgentProperties();
        properties.setModelProvider("openai");
        properties.setModelId("gpt-test");
        properties.setSystemPrompt("system");
        ToolRegistry toolRegistry = new ToolRegistry(new LinkedHashMap<>(), properties.getSecurity());
        StrandsObservability observability = new StrandsObservability(
                new SimpleMeterRegistry(), ObservationRegistry.NOOP, properties.getSecurity());
        StrandsAgent agent = new StrandsAgent(loop, toolRegistry, properties, List.of(), observability);
        if (sessionManager != null) {
            agent.setSessionManager(sessionManager);
        }
        return agent;
    }
}
