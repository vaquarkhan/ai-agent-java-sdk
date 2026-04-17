package com.example.spring.ai.agent.conversation;

import com.example.spring.ai.agent.execution.ExecutionMessage;
import com.example.spring.ai.agent.execution.AgentExecutionContext;
import java.util.List;

/**
 * Conversation manager that keeps only the last N messages (sliding window).
 *
 * <p>This is the simplest context management strategy: when the message list
 * exceeds the configured window size, older messages are trimmed from the front.
 *
 * @author Vaquar Khan
 */
public class SlidingWindowConversationManager implements ConversationManager {

    /** Default window size if none is specified. */
    public static final int DEFAULT_WINDOW_SIZE = 20;

    private final int windowSize;

    /**
     * Creates a sliding window conversation manager with the default window size of 20.
     */
    public SlidingWindowConversationManager() {
        this(DEFAULT_WINDOW_SIZE);
    }

    /**
     * Creates a sliding window conversation manager with the specified window size.
     *
     * @param windowSize the maximum number of messages to retain
     * @throws IllegalArgumentException if windowSize is less than 1
     */
    public SlidingWindowConversationManager(int windowSize) {
        if (windowSize < 1) {
            throw new IllegalArgumentException("windowSize must be >= 1, got " + windowSize);
        }
        this.windowSize = windowSize;
    }

    @Override
    public List<ExecutionMessage> manage(List<ExecutionMessage> messages, AgentExecutionContext context) {
        if (messages == null || messages.size() <= windowSize) {
            return messages;
        }
        return messages.subList(messages.size() - windowSize, messages.size());
    }

    /**
     * Returns the configured window size.
     *
     * @return the window size
     */
    public int getWindowSize() {
        return windowSize;
    }
}
