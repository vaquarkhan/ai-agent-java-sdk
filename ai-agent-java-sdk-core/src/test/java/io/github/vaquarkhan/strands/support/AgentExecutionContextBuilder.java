package io.github.vaquarkhan.strands.support;

/**
 * @author Vaquar Khan
 */
import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
import java.util.HashMap;
import java.util.Map;

public class AgentExecutionContextBuilder {
    private String sessionId = "session-1";
    private String userId = "user-1";
    private Map<String, String> headers = new HashMap<>();

    public AgentExecutionContextBuilder sessionId(String value) {
        this.sessionId = value;
        return this;
    }

    public AgentExecutionContextBuilder userId(String value) {
        this.userId = value;
        return this;
    }

    public AgentExecutionContextBuilder header(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public AgentExecutionContext build() {
        return new AgentExecutionContext(sessionId, userId, headers);
    }
}
