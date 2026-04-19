package io.github.vaquarkhan.strands.session;

import io.github.vaquarkhan.strands.execution.ExecutionMessage;
import java.util.List;

/**
 * Manages session persistence for agent conversations.
 *
 * <p>Implementations save and restore conversation messages across invocations,
 * enabling multi-turn conversations with state preservation.
 *
 * @author Vaquar Khan
 */
public interface SessionManager {

    /**
     * Save conversation messages for the given session.
     *
     * @param sessionId the session identifier
     * @param messages  the messages to persist
     */
    void save(String sessionId, List<ExecutionMessage> messages);

    /**
     * Load previously saved conversation messages for the given session.
     *
     * @param sessionId the session identifier
     * @return the saved messages, or an empty list if no session exists
     */
    List<ExecutionMessage> load(String sessionId);

    /**
     * Delete the session and its stored messages.
     *
     * @param sessionId the session identifier
     */
    void delete(String sessionId);

    /**
     * Check whether a session with the given identifier exists.
     *
     * @param sessionId the session identifier
     * @return true if the session exists
     */
    boolean exists(String sessionId);
}
