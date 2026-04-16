package com.example.spring.ai.strands.agent.conversation;

import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
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
    List<ExecutionMessage> manage(List<ExecutionMessage> messages, StrandsExecutionContext context);
}
