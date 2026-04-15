package com.example.spring.ai.strands.agent.support;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import java.util.HashMap;
import java.util.Map;

public class StrandsExecutionContextBuilder {
    private String sessionId = "session-1";
    private String userId = "user-1";
    private Map<String, String> headers = new HashMap<>();

    public StrandsExecutionContextBuilder sessionId(String value) {
        this.sessionId = value;
        return this;
    }

    public StrandsExecutionContextBuilder userId(String value) {
        this.userId = value;
        return this;
    }

    public StrandsExecutionContextBuilder header(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public StrandsExecutionContext build() {
        return new StrandsExecutionContext(sessionId, userId, headers);
    }
}
