package io.github.vaquarkhan.strands.conversation;

import io.github.vaquarkhan.strands.execution.ExecutionMessage;
import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Vaquar Khan
 */
class SlidingWindowConversationManagerTest {

    @Test
    void defaultWindowSizeIsTwenty() {
        SlidingWindowConversationManager manager = new SlidingWindowConversationManager();
        assertEquals(20, manager.getWindowSize());
    }

    @Test
    void customWindowSize() {
        SlidingWindowConversationManager manager = new SlidingWindowConversationManager(5);
        assertEquals(5, manager.getWindowSize());
    }

    @Test
    void windowSizeLessThanOneThrows() {
        assertThrows(IllegalArgumentException.class, () -> new SlidingWindowConversationManager(0));
        assertThrows(IllegalArgumentException.class, () -> new SlidingWindowConversationManager(-1));
    }

    @Test
    void messagesWithinWindowReturnedUnchanged() {
        SlidingWindowConversationManager manager = new SlidingWindowConversationManager(5);
        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "msg1"),
                new ExecutionMessage("user", "msg2"),
                new ExecutionMessage("user", "msg3"));

        List<ExecutionMessage> result = manager.manage(messages, context());
        assertEquals(3, result.size());
        assertEquals("msg1", result.get(0).content());
    }

    @Test
    void messagesExceedingWindowAreTrimmed() {
        SlidingWindowConversationManager manager = new SlidingWindowConversationManager(2);
        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "msg1"),
                new ExecutionMessage("user", "msg2"),
                new ExecutionMessage("user", "msg3"),
                new ExecutionMessage("user", "msg4"));

        List<ExecutionMessage> result = manager.manage(messages, context());
        assertEquals(2, result.size());
        assertEquals("msg3", result.get(0).content());
        assertEquals("msg4", result.get(1).content());
    }

    @Test
    void exactWindowSizeReturnedUnchanged() {
        SlidingWindowConversationManager manager = new SlidingWindowConversationManager(3);
        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "msg1"),
                new ExecutionMessage("user", "msg2"),
                new ExecutionMessage("user", "msg3"));

        List<ExecutionMessage> result = manager.manage(messages, context());
        assertEquals(3, result.size());
        assertEquals("msg1", result.get(0).content());
    }

    @Test
    void emptyListReturnedAsIs() {
        SlidingWindowConversationManager manager = new SlidingWindowConversationManager(5);
        List<ExecutionMessage> result = manager.manage(List.of(), context());
        assertEquals(0, result.size());
    }

    @Test
    void nullListReturnedAsNull() {
        SlidingWindowConversationManager manager = new SlidingWindowConversationManager(5);
        List<ExecutionMessage> result = manager.manage(null, context());
        assertNull(result);
    }

    @Test
    void windowSizeOneKeepsOnlyLastMessage() {
        SlidingWindowConversationManager manager = new SlidingWindowConversationManager(1);
        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "first"),
                new ExecutionMessage("assistant", "second"),
                new ExecutionMessage("user", "third"));

        List<ExecutionMessage> result = manager.manage(messages, context());
        assertEquals(1, result.size());
        assertEquals("third", result.get(0).content());
    }

    private AgentExecutionContext context() {
        return AgentExecutionContext.standalone("test-session");
    }
}
