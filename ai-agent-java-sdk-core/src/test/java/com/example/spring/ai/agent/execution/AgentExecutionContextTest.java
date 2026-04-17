package com.example.spring.ai.agent.execution;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.agent.support.AgentCoreContext;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentExecutionContextTest {

    @Test
    void headersAreImmutable() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "secret");
        AgentExecutionContext context = new AgentExecutionContext("s1", "u1", headers);
        assertThrows(UnsupportedOperationException.class, () -> context.getHeaders().put("x", "y"));
    }

    @Test
    void toStringDoesNotExposeHeaders() {
        AgentExecutionContext context = new AgentExecutionContext("s1", "u1", Map.of("Authorization", "secret"));
        assertTrue(context.toString().contains("s1"));
        assertTrue(!context.toString().contains("secret"));
    }

    @Test
    void equalsAndHashCodeExcludeHeaders() {
        AgentExecutionContext left = new AgentExecutionContext("s1", "u1", Map.of("a", "1"));
        AgentExecutionContext right = new AgentExecutionContext("s1", "u1", Map.of("a", "2"));
        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    void factoryMethodsWork() {
        AgentCoreContext agentCoreContext = new AgentCoreContext() {
            @Override
            public String sessionId() {
                return "sid";
            }

            @Override
            public String userId() {
                return "uid";
            }

            @Override
            public Map<String, String> headers() {
                return Map.of("h", "v");
            }
        };
        AgentExecutionContext fromRuntime = AgentExecutionContext.from(agentCoreContext);
        AgentExecutionContext standalone = AgentExecutionContext.standalone("standalone");
        assertEquals("sid", fromRuntime.getSessionId());
        assertEquals("standalone", standalone.getSessionId());
    }
}
