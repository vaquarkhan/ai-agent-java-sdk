package io.github.vaquarkhan.agent.conversation;

import io.github.vaquarkhan.agent.execution.ExecutionMessage;
import io.github.vaquarkhan.agent.execution.AgentExecutionContext;
import java.util.List;

/**
 * Manages the conversation context window before each model call.
 *
 * <p>Implementations can apply sliding window, token counting, summarization,
 * or other strategies to keep the message list within model context limits.
 *
 * @author Vaquar Khan
 */
public interface ConversationManager {

    /**
     * Apply context management to the message list.
     *
     * @param messages the current conversation messages (excluding system prompt)
     * @param context  the execution context
     * @return the managed message list
     */
    List<ExecutionMessage> manage(List<ExecutionMessage> messages, AgentExecutionContext context);
}
