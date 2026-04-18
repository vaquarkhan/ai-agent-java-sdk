package io.github.vaquarkhan.agent.conversation;

import io.github.vaquarkhan.agent.execution.ExecutionMessage;
import io.github.vaquarkhan.agent.execution.AgentExecutionContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Conversation manager that estimates token count per message and removes
 * oldest messages until the total fits within a configured token limit.
 *
 * <p>Token estimation uses a simple heuristic: characters / 4. This is a
 * reasonable approximation for English text with most LLM tokenizers.
 *
 * @author Vaquar Khan
 */
public class TokenCountConversationManager implements ConversationManager {

    /** Default maximum token count. */
    public static final int DEFAULT_MAX_TOKENS = 4096;

    /** Characters per token approximation. */
    static final int CHARS_PER_TOKEN = 4;

    private final int maxTokens;

    /**
     * Creates a token count conversation manager with the default limit of 4096 tokens.
     */
    public TokenCountConversationManager() {
        this(DEFAULT_MAX_TOKENS);
    }

    /**
     * Creates a token count conversation manager with the specified token limit.
     *
     * @param maxTokens the maximum estimated token count
     * @throws IllegalArgumentException if maxTokens is less than 1
     */
    public TokenCountConversationManager(int maxTokens) {
        if (maxTokens < 1) {
            throw new IllegalArgumentException("maxTokens must be >= 1, got " + maxTokens);
        }
        this.maxTokens = maxTokens;
    }

    @Override
    public List<ExecutionMessage> manage(List<ExecutionMessage> messages, AgentExecutionContext context) {
        if (messages == null || messages.isEmpty()) {
            return messages;
        }
        int totalTokens = messages.stream().mapToInt(this::estimateTokens).sum();
        if (totalTokens <= maxTokens) {
            return messages;
        }
        // Remove oldest messages until we fit within the limit
        List<ExecutionMessage> trimmed = new ArrayList<>(messages);
        while (trimmed.size() > 1 && totalTokens > maxTokens) {
            ExecutionMessage removed = trimmed.remove(0);
            totalTokens -= estimateTokens(removed);
        }
        return trimmed;
    }

    /**
     * Estimates the token count for a single message using chars / 4 approximation.
     *
     * @param message the message to estimate
     * @return the estimated token count
     */
    int estimateTokens(ExecutionMessage message) {
        if (message == null || message.content() == null) {
            return 0;
        }
        return Math.max(1, message.content().length() / CHARS_PER_TOKEN);
    }

    /**
     * Returns the configured maximum token count.
     *
     * @return the max tokens
     */
    public int getMaxTokens() {
        return maxTokens;
    }
}
