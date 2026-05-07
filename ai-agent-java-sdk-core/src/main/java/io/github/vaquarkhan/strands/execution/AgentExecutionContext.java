package io.github.vaquarkhan.strands.execution;

import io.github.vaquarkhan.strands.support.AgentCoreContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Vaquar Khan
 */

public final class AgentExecutionContext {

    private final String sessionId;
    private final String userId;
    private final Map<String, String> headers;

    public AgentExecutionContext(String sessionId, String userId, Map<String, String> headers) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.headers = new HashMap<>(headers == null ? Map.of() : headers);
    }

    public static AgentExecutionContext from(AgentCoreContext agentCoreContext) {
        Objects.requireNonNull(agentCoreContext, "agentCoreContext must not be null");
        return new AgentExecutionContext(
                agentCoreContext.sessionId(),
                agentCoreContext.userId(),
                agentCoreContext.headers());
    }

    public static AgentExecutionContext standalone(String sessionId) {
        return new AgentExecutionContext(sessionId, null, Map.of());
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    @Override
    public String toString() {
        return "AgentExecutionContext{sessionId='%s', userId='%s'}".formatted(sessionId, userId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AgentExecutionContext that)) {
            return false;
        }
        return Objects.equals(sessionId, that.sessionId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, userId);
    }
}
