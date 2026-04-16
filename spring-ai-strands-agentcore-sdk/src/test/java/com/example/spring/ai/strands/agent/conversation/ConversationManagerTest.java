package com.example.spring.ai.strands.agent.conversation;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.ModelTurnResponse;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionLoop;
import com.example.spring.ai.strands.agent.execution.StrandsLoopResult;
import com.example.spring.ai.strands.agent.observability.StrandsObservability;
import com.example.spring.ai.strands.agent.support.MockModelClient;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationManagerTest {

    @Test
    void slidingWindowConversationManagerKeepsLastNMessages() {
        // A simple sliding window that keeps only the last 2 messages
        ConversationManager slidingWindow = (messages, context) -> {
            if (messages.size() <= 2) {
                return messages;
            }
            return messages.subList(messages.size() - 2, messages.size());
        };

        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.finalAnswer("done"));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        loop.setConversationManager(slidingWindow);

        // Provide 4 user messages; the sliding window should trim to last 2
        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "msg1"),
                new ExecutionMessage("user", "msg2"),
                new ExecutionMessage("user", "msg3"),
                new ExecutionMessage("user", "msg4"));

        loop.run("sys", messages, emptyRegistry(), observability(), StrandsExecutionContext.standalone("s"));

        // The model should receive: system + last 2 messages (msg3, msg4)
        List<ExecutionMessage> sentToModel = modelClient.getCallHistory().get(0);
        assertEquals(3, sentToModel.size());
        assertEquals("system", sentToModel.get(0).role());
        assertEquals("msg3", sentToModel.get(1).content());
        assertEquals("msg4", sentToModel.get(2).content());
    }

    @Test
    void conversationManagerCalledBeforeEachModelCall() {
        // Conversation manager that appends a marker message
        java.util.concurrent.atomic.AtomicInteger callCount = new java.util.concurrent.atomic.AtomicInteger(0);
        ConversationManager countingManager = (messages, context) -> {
            callCount.incrementAndGet();
            return messages;
        };

        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.toolCall("calc", "{}"))
                .addResponse(ModelTurnResponse.finalAnswer("done"));
        ToolRegistry registry = new ToolRegistry(
                Map.of("calc", new com.example.spring.ai.strands.agent.support.TestToolCallback("calc", "c", a -> "4")),
                new StrandsAgentProperties.Security());
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        loop.setConversationManager(countingManager);

        loop.run("sys", List.of(new ExecutionMessage("user", "hi")),
                registry, observability(), StrandsExecutionContext.standalone("s"));

        // Two iterations = two model calls = conversation manager called twice
        assertEquals(2, callCount.get());
    }

    @Test
    void withoutConversationManagerAllMessagesPassedThrough() {
        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.finalAnswer("done"));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        // No conversation manager set

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "msg1"),
                new ExecutionMessage("user", "msg2"),
                new ExecutionMessage("user", "msg3"));

        loop.run("sys", messages, emptyRegistry(), observability(), StrandsExecutionContext.standalone("s"));

        // All 3 messages + system prompt should be sent
        List<ExecutionMessage> sentToModel = modelClient.getCallHistory().get(0);
        assertEquals(4, sentToModel.size());
        assertEquals("system", sentToModel.get(0).role());
        assertEquals("msg1", sentToModel.get(1).content());
        assertEquals("msg2", sentToModel.get(2).content());
        assertEquals("msg3", sentToModel.get(3).content());
    }

    private ToolRegistry emptyRegistry() {
        return new ToolRegistry(Map.of(), new StrandsAgentProperties.Security());
    }

    private StrandsObservability observability() {
        return new StrandsObservability(new SimpleMeterRegistry(), ObservationRegistry.NOOP,
                new StrandsAgentProperties.Security());
    }
}
