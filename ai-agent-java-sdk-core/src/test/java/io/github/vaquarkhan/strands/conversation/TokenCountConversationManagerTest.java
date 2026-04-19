package io.github.vaquarkhan.strands.conversation;

import io.github.vaquarkhan.strands.execution.ExecutionMessage;
import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vaquar Khan
 */
class TokenCountConversationManagerTest {

    @Test
    void defaultMaxTokensIs4096() {
        TokenCountConversationManager manager = new TokenCountConversationManager();
        assertEquals(4096, manager.getMaxTokens());
    }

    @Test
    void customMaxTokens() {
        TokenCountConversationManager manager = new TokenCountConversationManager(100);
        assertEquals(100, manager.getMaxTokens());
    }

    @Test
    void maxTokensLessThanOneThrows() {
        assertThrows(IllegalArgumentException.class, () -> new TokenCountConversationManager(0));
        assertThrows(IllegalArgumentException.class, () -> new TokenCountConversationManager(-1));
    }

    @Test
    void messagesWithinTokenLimitReturnedUnchanged() {
        // 4 chars / 4 = 1 token per message, 3 messages = 3 tokens, limit = 10
        TokenCountConversationManager manager = new TokenCountConversationManager(10);
        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "abcd"),
                new ExecutionMessage("user", "efgh"),
                new ExecutionMessage("user", "ijkl"));

        List<ExecutionMessage> result = manager.manage(messages, context());
        assertEquals(3, result.size());
    }

    @Test
    void messagesExceedingTokenLimitAreTrimmed() {
        // Each message is 40 chars = 10 tokens. 3 messages = 30 tokens. Limit = 15.
        TokenCountConversationManager manager = new TokenCountConversationManager(15);
        String longContent = "a".repeat(40); // 10 tokens
        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", longContent),
                new ExecutionMessage("user", longContent),
                new ExecutionMessage("user", longContent));

        List<ExecutionMessage> result = manager.manage(messages, context());
        // Should remove oldest until within limit. 30 > 15, remove first (20 > 15), remove second (10 <= 15)
        assertEquals(1, result.size());
    }

    @Test
    void emptyListReturnedAsIs() {
        TokenCountConversationManager manager = new TokenCountConversationManager(100);
        List<ExecutionMessage> result = manager.manage(List.of(), context());
        assertEquals(0, result.size());
    }

    @Test
    void nullListReturnedAsNull() {
        TokenCountConversationManager manager = new TokenCountConversationManager(100);
        List<ExecutionMessage> result = manager.manage(null, context());
        assertNull(result);
    }

    @Test
    void estimateTokensUsesCharsDividedByFour() {
        TokenCountConversationManager manager = new TokenCountConversationManager(100);
        // 20 chars / 4 = 5 tokens
        assertEquals(5, manager.estimateTokens(new ExecutionMessage("user", "a".repeat(20))));
        // 1 char / 4 = 0, but minimum is 1
        assertEquals(1, manager.estimateTokens(new ExecutionMessage("user", "a")));
    }

    @Test
    void nullContentEstimatesZeroTokens() {
        TokenCountConversationManager manager = new TokenCountConversationManager(100);
        assertEquals(0, manager.estimateTokens(new ExecutionMessage("user", null)));
        assertEquals(0, manager.estimateTokens(null));
    }

    @Test
    void alwaysKeepsAtLeastOneMessage() {
        // Even if the single message exceeds the limit, keep it
        TokenCountConversationManager manager = new TokenCountConversationManager(1);
        String longContent = "a".repeat(100); // 25 tokens, way over limit of 1
        List<ExecutionMessage> messages = List.of(new ExecutionMessage("user", longContent));

        List<ExecutionMessage> result = manager.manage(messages, context());
        assertEquals(1, result.size());
    }

    @Test
    void removesOldestMessagesFirst() {
        // 3 messages: 10 tokens, 10 tokens, 10 tokens. Limit = 25.
        TokenCountConversationManager manager = new TokenCountConversationManager(25);
        String content = "a".repeat(40); // 10 tokens each
        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", content),
                new ExecutionMessage("assistant", content),
                new ExecutionMessage("user", content));

        List<ExecutionMessage> result = manager.manage(messages, context());
        // 30 > 25, remove first -> 20 <= 25
        assertEquals(2, result.size());
        assertEquals("assistant", result.get(0).role());
        assertEquals("user", result.get(1).role());
    }

    private AgentExecutionContext context() {
        return AgentExecutionContext.standalone("test-session");
    }
}
