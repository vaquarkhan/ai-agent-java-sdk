package com.example.spring.ai.strands.agent.session;

import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session manager backed by a {@link ConcurrentHashMap}.
 *
 * <p>Useful for development and testing. For production use with persistence,
 * consider {@link FileSessionManager} or a database-backed implementation
 * (e.g., DynamoDB - see extension point documentation).
 *
 * <p>Note: DynamoDB session manager is out of scope for this module since it
 * requires the AWS SDK dependency. It can be implemented as an extension by
 * adding the AWS SDK and implementing the {@link SessionManager} interface.
 *
 * @author Vaquar Khan
 */
public class InMemorySessionManager implements SessionManager {

    private final ConcurrentHashMap<String, List<ExecutionMessage>> store = new ConcurrentHashMap<>();

    @Override
    public void save(String sessionId, List<ExecutionMessage> messages) {
        store.put(sessionId, new ArrayList<>(messages));
    }

    @Override
    public List<ExecutionMessage> load(String sessionId) {
        List<ExecutionMessage> messages = store.get(sessionId);
        return messages != null ? new ArrayList<>(messages) : List.of();
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
