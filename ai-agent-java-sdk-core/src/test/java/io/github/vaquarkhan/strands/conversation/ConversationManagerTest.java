package io.github.vaquarkhan.strands.conversation;

/**
 * @author Vaquar Khan
 */
import io.github.vaquarkhan.strands.config.AiAgentProperties;
import io.github.vaquarkhan.strands.execution.ExecutionMessage;
import io.github.vaquarkhan.strands.execution.ModelTurnResponse;
import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
import io.github.vaquarkhan.strands.execution.AgentExecutionLoop;
import io.github.vaquarkhan.strands.execution.AgentLoopResult;
import io.github.vaquarkhan.strands.observability.AgentObservability;
import io.github.vaquarkhan.strands.support.MockModelClient;
import io.github.vaquarkhan.strands.tool.ToolRegistry;
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
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        loop.setConversationManager(slidingWindow);

        // Provide 4 user messages; the sliding window should trim to last 2
        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "msg1"),
                new ExecutionMessage("user", "msg2"),
                new ExecutionMessage("user", "msg3"),
                new ExecutionMessage("user", "msg4"));

        loop.run("sys", messages, emptyRegistry(), observability(), AgentExecutionContext.standalone("s"));

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
                Map.of("calc", new io.github.vaquarkhan.strands.support.TestToolCallback("calc", "c", a -> "4")),
                new AiAgentProperties.Security());
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        loop.setConversationManager(countingManager);

        loop.run("sys", List.of(new ExecutionMessage("user", "hi")),
                registry, observability(), AgentExecutionContext.standalone("s"));

        // Two iterations = two model calls = conversation manager called twice
        assertEquals(2, callCount.get());
    }

    @Test
    void withoutConversationManagerAllMessagesPassedThrough() {
        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.finalAnswer("done"));
        AgentExecutionLoop loop = new AgentExecutionLoop(modelClient, 5, "agent");
        // No conversation manager set

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "msg1"),
                new ExecutionMessage("user", "msg2"),
                new ExecutionMessage("user", "msg3"));

        loop.run("sys", messages, emptyRegistry(), observability(), AgentExecutionContext.standalone("s"));

        // All 3 messages + system prompt should be sent
        List<ExecutionMessage> sentToModel = modelClient.getCallHistory().get(0);
        assertEquals(4, sentToModel.size());
        assertEquals("system", sentToModel.get(0).role());
        assertEquals("msg1", sentToModel.get(1).content());
        assertEquals("msg2", sentToModel.get(2).content());
        assertEquals("msg3", sentToModel.get(3).content());
    }

    private ToolRegistry emptyRegistry() {
        return new ToolRegistry(Map.of(), new AiAgentProperties.Security());
    }

    private AgentObservability observability() {
        return new AgentObservability(new SimpleMeterRegistry(), ObservationRegistry.NOOP,
                new AiAgentProperties.Security());
    }
}
